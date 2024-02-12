package com.example.ctftime_planner

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ctftime_planner.ui.theme.CTFtime_PlannerTheme
import kotlinx.coroutines.Dispatchers
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
            TextButton(onClick = { expanded = true }) { // Set expanded state to true when clicked
                Text("Select Timezone")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false } // Set expanded state to false when dismissed
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
    NavHost(navController = navController, startDestination = NavigationItem.Home.route) {

        composable(NavigationItem.Home.route) {
            Home(sharedPref)
        }

        composable(NavigationItem.NowRunning.route) {
            NowRunning()
        }

        composable(NavigationItem.Upcoming.route) {
            Upcoming(sharedPref)
        }

        composable(NavigationItem.Calendar.route) {
            Calendar()
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

                items.forEach {
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

                        })
                }
            }
        }
    ) {
        NavigationController(navController = navController, sharedPref = sharedPref)
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun NowRunning() {

    Column(
        modifier=Modifier.fillMaxSize(),
        verticalArrangement=Arrangement.Center,
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Column(
            modifier=Modifier.fillMaxSize(),
            verticalArrangement=Arrangement.Center,
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Text(text = "Now Running")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Upcoming(sharedPref: SharedPreferences) {

    // Initialize a mutable state to hold the list of events
    var events by remember { mutableStateOf<List<EventItems>>(emptyList()) }

    val selectedTimeZone = sharedPref.getString("selectedTimeZone", "")

    // Fetch event data from API
    LaunchedEffect(Unit) {
        fetchEvents { fetchedEvents ->
            events = fetchedEvents
        }
    }

    // Display the fetched event information in a scrollable list
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
                    text = "Start: ${startTime}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 80.dp)
                )
                Text(
                    text = "End: ${endTime}",
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
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Calendar() {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        val daysWithDates = generateDaysWithDates(currentYearMonth)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 56.dp)
        ) {
            // Add table headers as the first row
            item {
                HeaderRow()
            }
            // Add each week's days
            itemsIndexed(daysWithDates.chunked(7)) { index, week ->
                WeekRow(week, selectedMonth = currentYearMonth)
            }
        }
    }
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
fun WeekRow(week: List<Pair<String, LocalDate?>>, selectedMonth: YearMonth) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((dayName, date) in week) {
            val textColor = if (date != null && date.month == selectedMonth.month)
                Color(0xFF40E0D0) // Color for days in the selected month
            else
                Color.LightGray // Default color for other days

            Text(
                text = if (date != null) "${date.dayOfMonth}" else "",
                color = textColor,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .border(1.dp, Color(0xFF40E0D0), shape = RoundedCornerShape(4.dp))
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateDaysWithDates(yearMonth: YearMonth): List<Pair<String, LocalDate?>> {
    val daysWithDates = mutableListOf<Pair<String, LocalDate?>>()
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val startDay = if (firstDayOfWeek == DayOfWeek.MONDAY.value) 1 else 8 - firstDayOfWeek
    val lastDayOfMonth = yearMonth.lengthOfMonth()
    var currentDay = startDay

    // Fill in the days from the previous month if the month does not start with Monday
    if (startDay > 1) {
        val prevMonth = yearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val daysToFill = firstDayOfWeek - 1
        for (i in 1..daysToFill) {
            val prevDate = prevMonth.atDay(daysInPrevMonth - (daysToFill - i))
            daysWithDates.add("" to prevDate)
        }
        // Reset currentDay to 1 after filling in the days from the previous month
        currentDay = 1
    }

    // Fill in the days of the current month
    while (currentDay <= lastDayOfMonth) {
        val currentDate = yearMonth.atDay(currentDay)
        daysWithDates.add("" to currentDate)
        currentDay++
    }

    // Fill in the days from the next month to complete the last week
    if (lastDayOfMonth + firstDayOfWeek.toInt() - 1 != 35) {
        val remainingDays = 7 - (daysWithDates.size % 7)
        val nextMonth = yearMonth.plusMonths(1)
        val nextMonthFirstDay = nextMonth.atDay(1)
        for (i in 1..remainingDays) {
            val nextDate = nextMonthFirstDay.plusDays(i.toLong() - 1)
            daysWithDates.add("" to nextDate)
        }
    }

    return daysWithDates
}

fun getAllTimezones(): List<String> {
    return TimeZone.getAvailableIDs().toList()
}

@RequiresApi(Build.VERSION_CODES.O)
fun convertDateTimeToTimeZone(dateTimeString: String, timeZone: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    val dateTime = ZonedDateTime.parse(dateTimeString, formatter)

    // Convert to the desired timezone
    val zoneId = java.time.ZoneId.of(timeZone)
    val convertedDateTime = dateTime.withZoneSameInstant(zoneId)

    // Format the datetime in the desired format
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    return outputFormatter.format(convertedDateTime)
}

// Function to parse event titles from JSON response
fun parseEventsFromJson(json: String): List<EventItems> {
    val events = mutableListOf<EventItems>()
    val jsonArray = JSONArray(json)
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        // Load needed values form json Object
        val title = jsonObject.getString("title")
        val weight = jsonObject.getString("weight")
        val format = jsonObject.getString("format")
        val id = jsonObject.getString("id")
        val start = jsonObject.getString("start")
        val end = jsonObject.getString("finish")

        // get duration
        val durationObjects = jsonObject.getString("duration")
        val durationJsonObj = JSONObject(durationObjects)

        val hours = durationJsonObj.getInt("hours")
        val days = durationJsonObj.getInt("days")
        val totalHours = hours + (days * 24)

        // Check if event is online of offline/on-site
        val location = if (jsonObject.getString("onsite") == "true") "On-Site" else "Online"

        // return Items with properties
        events.add(EventItems(title, weight, format, start, end, totalHours, location, id))
    }
    return events
}

// Function to fetch events from the API
suspend fun fetchEvents(onEventsFetched: (List<EventItems>) -> Unit) {
    withContext(Dispatchers.IO) {
        val url = URL("https://ctftime.org/api/v1/events/?limit=100")
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

            // Parse the JSON response and extract event titles
            val events = parseEventsFromJson(response.toString())
            onEventsFetched(events)
        } else {
            // Handle error if request is not successful
        }
        connection.disconnect()
    }
}