package com.onodatatable.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onodatatable.core.DataTableLayout
import com.onodatatable.app.ui.theme.OnodatatableTheme

private const val ROW_COUNT = 40
private const val COLUMN_COUNT = 21

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnodatatableTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DataTableLayout<String, String, String>(
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        topContent {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Top Content Above Data Table", fontSize = 20.sp)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    repeat(20) {
                                        Text(
                                            modifier = Modifier.padding(8.dp),
                                            text = "Item #$it"
                                        )
                                    }
                                }
                                repeat(10) {
                                    Text(text = "This is a long random line of text #$it to test top content scrolling behavior.")
                                }
                                Text(modifier = Modifier.padding(top = 16.dp), text = "#### DATA TABLE ###", fontSize = 20.sp)
                            }
                        }

                        bottomContent {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Bottom Content Below Data Table", fontSize = 20.sp)
                                repeat(10) {
                                    Text(text = "This is a long random line of text #$it to test bottom content scrolling behavior.")
                                }
                            }
                        }
                        
                        columnHeaders(generateListData(size = COLUMN_COUNT, label = "column")) { data ->
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = data,
                                fontSize = 16.sp,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        rowHeaders(generateListData(ROW_COUNT, label = "row")) { data ->
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = data,
                                fontSize = 16.sp,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        cells(generateMatrixData(ROW_COUNT, columns = COLUMN_COUNT)) { data ->
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = data,
                                fontSize = 16.sp,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateMatrixData(rows: Int = 10, columns: Int = 10): List<List<String>> = List(rows) { rowIndex ->
        List(columns) { columnIndex ->
            "Cell${rowIndex + 1}${columnIndex + 1}"
        }
    }

    private fun generateListData(size: Int = 10, label: String): List<String> = List(size) { index ->
        "$label${index + 1}"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OnodatatableTheme {
        Greeting("Android")
    }
}
