package com.example.ctftime_planner

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ctftime_planner.ui.theme.CTFtime_PlannerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CTFtime_PlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Navigation()
                }
            }
        }
    }
}

@Composable
fun Home(sharedPref: SharedPreferences) {
    var selectedTimeZone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) } // State to control dropdown visibility
    val timeZones = getAllTimezones()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        selectedTimeZone = sharedPref.getString("selectedTimeZone", "") ?: ""

        // Dropdown menu
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("Select Timezone")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                timeZones.forEach { timeZone ->
                    DropdownMenuItem(onClick = {
                        selectedTimeZone = timeZone
                        expanded = false
                        // Save selected timezone to SharedPreferences
                        with(sharedPref.edit()) {
                            putString("selectedTimeZone", timeZone)
                            apply()
                        }
                    }) {
                        Text(text = timeZone)
                    }
                }
            }
        }
        // Display selected timezone
        if (selectedTimeZone.isNotEmpty()) {
            Text(text = "Selected Timezone: $selectedTimeZone")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationController(navController: NavHostController, sharedPref: SharedPreferences) {

    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val eventDao = db.eventDao()

    NavHost(navController = navController, startDestination = NavigationItem.Home.route) {

        composable(NavigationItem.Home.route) {
            Home(sharedPref)
        }

        composable(NavigationItem.NowRunning.route) {
            NowRunning(sharedPref, eventDao)
        }

        composable(NavigationItem.Upcoming.route) {
            Upcoming(sharedPref)
        }

        composable(NavigationItem.Calendar.route) {
            Calendar(sharedPref, eventDao)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun Navigation() {

    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

    val items = listOf(
        NavigationItem.Home,
        NavigationItem.NowRunning,
        NavigationItem.Upcoming,
        NavigationItem.Calendar
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "CTF-Planner") },
                backgroundColor = MaterialTheme.colors.background
            )},
        bottomBar = {
            BottomNavigation(backgroundColor = MaterialTheme.colors.background) {

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { it ->
                    BottomNavigationItem(
                        selected = currentRoute == it.route,
                        label = {
                            Text(
                                text = it.label,
                                color = if (currentRoute==it.route) Color.DarkGray else Color.LightGray)
                        },
                        icon = {
                            Icon(
                                imageVector = it.icons,
                                contentDescription = null,
                                tint = if (currentRoute==it.route) Color.DarkGray else Color.LightGray)
                        },
                        onClick = {
                            if (currentRoute!=it.route) {

                                navController.graph?.startDestinationRoute?.let {
                                    navController.popBackStack(it, true)
                                }

                                navController.navigate(it.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        NavigationController(navController = navController, sharedPref = sharedPref)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun NowRunning(sharedPref: SharedPreferences, eventDao: EventDao) {

    var events by remember { mutableStateOf<List<EventItems>>(emptyList()) }

    val selectedTimeZone = sharedPref.getString("selectedTimeZone", "")

    val context = LocalContext.current

    val db = AppDatabase.getDatabase(context)
    val eventDao = db.eventDao()

    LaunchedEffect(Unit) {
        fetchEvents(url = URL("https://aryt3.dev/api/running_events")) { fetchedEvents ->
            events = fetchedEvents
        }
    }

    fun addEvent(event: EventItems) {
        val eventEntity = EventEntity(
            title = event.title,
            weight = event.weight.toFloat(),
            start = event.start,
            end = event.end,
            eventId = event.id
        )

        CoroutineScope(Dispatchers.IO).launch {
            val existingEvent = eventDao.getEventById(event.id)
            if (existingEvent == null) {
                // If event does not exist in the db, insert it
                eventDao.insertEvent(eventEntity)
            }
        }
    }

    fun removeEvent(eventId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.deleteEventById(eventId)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 56.dp)
    ) {
        items(events) { event ->

            val startTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.start, it) }
            val endTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.end, it) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .border(border = BorderStroke(1.dp, Color.Black))
                    .padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Black,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Weight: ${event.weight}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 30.dp)
                )

                Text(
                    text = "Format: ${event.format}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 50.dp)
                )

                Text(
                    text = "Start: $startTime",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 80.dp)
                )

                Text(
                    text = "End: $endTime",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 100.dp)
                )

                Text(
                    text = "Duration: ${event.duration} hours",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 120.dp)
                )

                Text(
                    text = "Location: ${event.location}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 150.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            // Add event item
                            addEvent(event)
                        },
                        modifier = Modifier
                            .padding(top = 120.dp, end = 8.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Green)
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.Green
                        )
                    }
                    IconButton(
                        onClick = {
                            // Remove event item
                            removeEvent(event.id)
                        },
                        modifier = Modifier
                            .padding(top = 120.dp, end = 8.dp)
                            .clip(CircleShape)
                            .animateContentSize()
                            .border(1.dp, Color.Red)
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Remove",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Upcoming(sharedPref: SharedPreferences) {

    var events by remember { mutableStateOf<List<EventItems>>(emptyList()) }

    val selectedTimeZone = sharedPref.getString("selectedTimeZone", "")

    val context = LocalContext.current

    val db = AppDatabase.getDatabase(context)
    val eventDao = db.eventDao()

    // Fetch event data from API
    LaunchedEffect(Unit) {
        fetchEvents(url = URL("https://ctftime.org/api/v1/events/?limit=100")) { fetchedEvents ->
            events = fetchedEvents
        }
    }

    fun addEvent(event: EventItems) {
        val eventEntity = EventEntity(
            title = event.title,
            weight = event.weight.toFloat(),
            start = event.start,
            end = event.end,
            eventId = event.id
        )

        CoroutineScope(Dispatchers.IO).launch {
            val existingEvent = eventDao.getEventById(event.id)
            if (existingEvent == null) {
                // If event does not exist in the db, insert it
                eventDao.insertEvent(eventEntity)
            }
        }
    }

    fun removeEvent(eventId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            eventDao.deleteEventById(eventId)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 56.dp)
    ) {
        items(events) { event ->

            val startTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.start, it) }
            val endTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.end, it) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .border(border = BorderStroke(1.dp, Color.Black))
                    .padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Black,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Weight: ${event.weight}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 30.dp)
                )

                Text(
                    text = "Format: ${event.format}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 50.dp)
                )

                Text(
                    text = "Start: $startTime",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 80.dp)
                )

                Text(
                    text = "End: $endTime",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 100.dp)
                )

                Text(
                    text = "Duration: ${event.duration} hours",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 120.dp)
                )

                Text(
                    text = "Location: ${event.location}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 150.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            // Add event item
                            addEvent(event)
                        },
                        modifier = Modifier
                            .padding(top = 120.dp, end = 8.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Green)
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.Green
                        )
                    }
                    IconButton(
                        onClick = {
                            // Remove event item
                            removeEvent(event.id)
                        },
                        modifier = Modifier
                            .padding(top = 120.dp, end = 8.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Red)
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Remove",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Calendar(sharedPref: SharedPreferences, eventDao: EventDao) {

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val selectedDate = remember { mutableStateOf<LocalDate?>(null) } // State var for selected date
    var eventsForSelectedDate by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    val selectedTimeZone = sharedPref.getString("selectedTimeZone", "")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
            }

            Text(
                text = "${currentYearMonth.month.name} ${currentYearMonth.year}",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 5.dp)
            )

            IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            val daysWithDates = remember { mutableStateOf<List<Pair<String, LocalDate?>>?>(null) }

            // Fetch events from db and generate days with dates
            LaunchedEffect(currentYearMonth) {
                val days = generateDaysWithDates(currentYearMonth, eventDao)
                daysWithDates.value = days
            }

            daysWithDates.value?.let { days ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 56.dp)
                ) {
                    item {
                        HeaderRow()
                    }
                    itemsIndexed(days.chunked(7)) { index, week ->
                        WeekRow(
                            week,
                            selectedMonth = currentYearMonth,
                            onDayClick = { selectedDate.value = it } // Update selected date
                        )
                    }
                }
            }

            LaunchedEffect(selectedDate.value) {
                selectedDate.value?.let { date ->
                    val events = eventDao.getEventsForDate("${date.toString()}T00:00:00", "${date.toString()}T23:59:59") // Der Pfusch muass da wurscht sein
                    eventsForSelectedDate = events
                }
            }

            // Show selected date and events for that day
            selectedDate.value?.let { selectedDate ->
                val dayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value
                val adjustedDayOfWeek = if (dayOfWeek == 7) 1 else dayOfWeek

                Text(
                    text = "Selected Date: ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(
                        top = if ((adjustedDayOfWeek > 5 && currentYearMonth.lengthOfMonth() == 31) || (adjustedDayOfWeek > 6 && currentYearMonth.lengthOfMonth() == 30)) 220.dp else 190.dp,
                        start = 5.dp
                    )
                )

                Text(
                    text = if (eventsForSelectedDate.isEmpty()) "No events on this day." else "Events:",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    ),
                    modifier = Modifier
                        .padding(
                            top = 245.dp,
                            start = 5.dp
                        )
                )

                if (eventsForSelectedDate.isNotEmpty()) {
                    Box(modifier = Modifier
                        .padding(top = 270.dp, bottom = 56.dp)
                        .background(color = Color.White)
                        .border(border = BorderStroke(1.dp, Color.Black))
                    ) {
                        LazyColumn {
                            items(eventsForSelectedDate) { event ->
                                val startTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.start, it) }
                                val endTime = selectedTimeZone?.let { convertDateTimeToTimeZone(event.end, it) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .border(border = BorderStroke(1.dp, Color.Black))
                                ) {
                                    EventRow(
                                        event = event,
                                        modifier = Modifier.padding(start = 5.dp, top = 5.dp),
                                        start = startTime,
                                        end = endTime
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(event: EventEntity, modifier: Modifier = Modifier, start: String?, end: String?) {

    Text(
        text = event.title,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color.Black,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )

    Text(
        text = "Start: $start",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color.Gray
        ),
        modifier = Modifier.padding(top = 30.dp, start = 5.dp)
    )

    Text(
        text = "End: $end",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color.Gray
        ),
        modifier = Modifier.padding(top = 50.dp, start = 5.dp)
    )

    Text(
        text = "Weight: ${event.weight}",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color.Gray
        ),
        modifier = Modifier.padding(top = 80.dp, start = 5.dp)
    )
}

@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
            Text(
                text = dayName,
                color = Color(0xFF40E0D0),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .border(1.dp, Color(0xFF40E0D0), shape = RoundedCornerShape(4.dp))
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeekRow(
    week: List<Pair<String, LocalDate?>>,
    selectedMonth: YearMonth,
    onDayClick: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((dayName, date) in week) {
            val textColor = if (date != null && date.month == selectedMonth.month)
                if (dayName == "red") Color.Red else Color(0xFF40E0D0)
            else
                Color.LightGray

            val dayOfMonth = date?.dayOfMonth ?: 0

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clickable {
                        date?.let { onDayClick(it) }
                    }
                    .border(1.dp, Color(0xFF40E0D0), shape = RoundedCornerShape(4.dp))
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$dayOfMonth",
                    color = textColor,
                    style = TextStyle(
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun generateDaysWithDates(yearMonth: YearMonth, eventDao: EventDao): List<Pair<String, LocalDate?>> {
    val daysWithDates = mutableListOf<Pair<String, LocalDate?>>()
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val startDay = if (firstDayOfWeek == DayOfWeek.MONDAY.value) 1 else 8 - firstDayOfWeek
    val lastDayOfMonth = yearMonth.lengthOfMonth()
    var currentDay = startDay

    // Fill days from the previous month if the month does not start with Monday
    if (startDay > 1) {
        val prevMonth = yearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val daysToFill = firstDayOfWeek - 1
        for (i in 1..daysToFill) {
            val prevDate = prevMonth.atDay(daysInPrevMonth - (daysToFill - i))
            daysWithDates.add("" to prevDate)
        }
        // Reset currentDay to 1 after filling in the days from previous month
        currentDay = 1
    }

    // Fill with days of the current month
    while (currentDay <= lastDayOfMonth) {
        val currentDate = yearMonth.atDay(currentDay)
        daysWithDates.add("" to currentDate)
        currentDay++
    }

    // Fill days from the next month to complete the last week
    if (lastDayOfMonth + firstDayOfWeek.toInt() - 1 != 35) {
        val remainingDays = 7 - (daysWithDates.size % 7)
        val nextMonth = yearMonth.plusMonths(1)
        val nextMonthFirstDay = nextMonth.atDay(1)
        for (i in 1..remainingDays) {
            val nextDate = nextMonthFirstDay.plusDays(i.toLong() - 1)
            daysWithDates.add("" to nextDate)
        }
    }

    // Fetch events from db
    val events = eventDao.getAllEvents()

    // check if event takes place on a day
    for ((index, day) in daysWithDates.withIndex()) {
        if (day.second != null) {
            val eventDate = day.second
            val isEventScheduled = events.any { event ->
                val eventStartDate = LocalDate.parse(event.start.split("T")[0])
                val eventEndDate = LocalDate.parse(event.end.split("T")[0])
                eventStartDate <= eventDate && eventEndDate >= eventDate
            }
            if (isEventScheduled) {
                daysWithDates[index] = "red" to day.second
            }
        }
    }

    return daysWithDates
}

// get all time zones from inbuilt function
fun getAllTimezones(): List<String> {
    return TimeZone.getAvailableIDs().toList()
}

@RequiresApi(Build.VERSION_CODES.O)
fun convertDateTimeToTimeZone(dateTimeString: String, timeZone: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    val dateTime = ZonedDateTime.parse(dateTimeString, formatter)

    // Convert to desired timezone
    val zoneId = java.time.ZoneId.of(timeZone)
    val convertedDateTime = dateTime.withZoneSameInstant(zoneId)

    // Format datetime in desired format
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return outputFormatter.format(convertedDateTime)
}

// Function to parse event titles from JSON response
fun parseEventsFromJson(json: String): List<EventItems> {
    val events = mutableListOf<EventItems>()
    val jsonArray = JSONArray(json)
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)

        // get basic properties
        val title = jsonObject.getString("title")
        val weight = jsonObject.getString("weight")
        val format = jsonObject.getString("format")
        val eventId = jsonObject.getString("id")
        val start = jsonObject.getString("start")
        val end = jsonObject.getString("finish")

        // get duration
        val durationObjects = jsonObject.getString("duration")
        val durationJsonObj = JSONObject(durationObjects)

        val hours = durationJsonObj.getInt("hours")
        val days = durationJsonObj.getInt("days")
        val totalHours = hours + (days * 24)

        // Check if event is online or offline/on-site
        val location = if (jsonObject.getString("onsite") == "true") "On-Site" else "Online"

        // return Items with properties
        events.add(EventItems(title, weight, format, start, end, totalHours, location, eventId))
    }
    return events
}

// Function to fetch events from the API
suspend fun fetchEvents(url: URL, onEventsFetched: (List<EventItems>) -> Unit) {
    withContext(Dispatchers.IO) {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val events = parseEventsFromJson(response.toString())
            onEventsFetched(events)
        }
        connection.disconnect()
    }
}
