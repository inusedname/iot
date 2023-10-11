package dev.vstd.myiot.screen.analytic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.vstd.myiot.R
import dev.vstd.myiot.data.Singleton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Destination
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun detail_(navigator: DestinationsNavigator) {
    val vimel: DetailVimel = viewModel()
    val activity = LocalContext.current as FragmentActivity

    val uiData by vimel.uiData.collectAsStateWithLifecycle()

    var showFilterDialog by remember { mutableStateOf(false) }
    var initTimes by remember {
        mutableStateOf(Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -7)
        }.timeInMillis to System.currentTimeMillis())
    }
    var initValues by remember { mutableStateOf(0f to 100f) }

    LaunchedEffect(true) {
        vimel.setData(Singleton.rawMessage)
    }

    Scaffold(topBar = {
        TopAppBar(navigationIcon = {
            IconButton(onClick = { navigator.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, null)
            }
        }, title = { Text(text = "Analytics") }, actions = {
            IconButton(onClick = vimel::sortByTime) {
                Icon(imageVector = Icons.Rounded.Sort, contentDescription = null)
            }
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(imageVector = Icons.Rounded.FilterAlt, contentDescription = null)
            }
        })
    }) {
        Column(Modifier.padding(it)) {
            LazyColumn {
                items(uiData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = it.first.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = it.second.toDDMMYYYYHHMM())
                    }
                    Divider()
                }
            }
        }
    }
    if (showFilterDialog) {
        _filter_dialog(activity, initTimes, initValues, onDismiss = {
            showFilterDialog = false
        }) { timeRange, valueRange ->
            initTimes = timeRange
            initValues = valueRange
            vimel.filter(timeRange, valueRange)
            showFilterDialog = false
        }
    }
}

fun Long.toDDMMYYYYHHMM(): String {
    val date = java.util.Date(this)
    val format = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    return format.format(date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun _filter_dialog(
    activity: FragmentActivity,
    initTimes: Pair<Long, Long>,
    initValues: Pair<Float, Float>,
    onDismiss: () -> Unit,
    onComplete: (Pair<Long, Long>, Pair<Float, Float>) -> Unit
) {
    var initTimes by remember { mutableStateOf(initTimes) }
    var initValues by remember {
        mutableStateOf(
            Pair(
                initValues.first.toString(),
                initValues.second.toString()
            )
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = "Filter Dialog", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Time Range", style = MaterialTheme.typography.labelMedium)
                _time_button(activity = activity, init = initTimes, onDone = { initTimes = it })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = "Threshold", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedTextField(value = initValues.first, onValueChange = {
                    initValues = initValues.copy(first = it)
                }, modifier = Modifier.width(80.dp))
                Text(text = " - ")
                OutlinedTextField(value = initValues.second, onValueChange = {
                    initValues = initValues.copy(second = it)
                }, modifier = Modifier.width(80.dp))
            }
            Button(
                enabled = initValues.first.toFloatOrNull() != null && initValues.second.toFloatOrNull() != null,
                onClick = {
                    onComplete(
                        initTimes,
                        Pair(initValues.first.toFloat(), initValues.second.toFloat())
                    )
                }, modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
            ) {
                Text(text = "Filter")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun _time_button(
    activity: FragmentActivity,
    init: Pair<Long, Long>,
    onDone: (Pair<Long, Long>) -> Unit
) {
    AssistChip(
        modifier = Modifier.padding(start = 8.dp),
        onClick = {
            showDatePickerDialog(activity, init, onDone)
        },
        label = {
            Text(
                text = init.toDateRangeString(),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.FilterAlt,
                contentDescription = "alarm"
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primary,
            labelColor = Color.White,
            trailingIconContentColor = Color.White
        ),
        border = null
    )
}

fun Pair<Long, Long>.toDateRangeString(): String {
    return SimpleDateFormat("MMM dd,yyyy", Locale.getDefault()).run {
        format(first) + " - " + format(second)
    }
}

fun showDatePickerDialog(
    activity: FragmentActivity,
    init: Pair<Long, Long>,
    onDone: (Pair<Long, Long>) -> Unit
) {
    val offset = TimeZone.getDefault().rawOffset
    MaterialDatePicker.Builder.dateRangePicker()
        .setTheme(R.style.ThemeMaterialCalendar)
        .setSelection(androidx.core.util.Pair(init.first + offset, init.second + offset))
        .build()
        .apply {
            addOnPositiveButtonClickListener {
                val first = Calendar.getInstance()
                first.timeInMillis = it.first
                first.set(Calendar.HOUR_OF_DAY, 0)
                first.set(Calendar.MINUTE, 1)
                val second = Calendar.getInstance()
                second.timeInMillis = it.second
                second.set(Calendar.HOUR_OF_DAY, 23)
                second.set(Calendar.MINUTE, 59)
                onDone(first.timeInMillis to second.timeInMillis)
            }
            addOnNegativeButtonClickListener {
                dismiss()
            }
            show(activity.supportFragmentManager, "date_picker")
        }
}