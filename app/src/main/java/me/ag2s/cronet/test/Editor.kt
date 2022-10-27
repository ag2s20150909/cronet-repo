package me.ag2s.cronet.test

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private val ELEMENT_HEIGHT = 48.dp

@Composable
fun Editor(navController: NavController, screen: Screen, viewModel: TestViewModel) {
    val txt by viewModel.txt.collectAsState()
    val host by viewModel.url.collectAsState()
    val method by viewModel.httpMethod.collectAsState()
    val protocol by viewModel.protocol.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.getHtml()

    }



    Column(modifier = Modifier.fillMaxSize()) {


        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Request Method", modifier = Modifier.weight(1.0f))


            DropDownSpinner(
                selectedItem = method,
                onItemSelected = { index, _ -> viewModel.changeMethod(index) },
                itemList = RequestState.httpMethodS
            )

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            DropDownSpinner(
                modifier = Modifier.width(110.dp),
                selectedItem = protocol,
                onItemSelected = { _, item ->
                    viewModel.changeProtocol(item)
                    viewModel.getHtml()
                },
                itemList = RequestState.protocols
            )
            TextField(
                modifier = Modifier.weight(1.0f),
                value = host,
                onValueChange = { view ->
                    viewModel.changeHost(view)

                })
        }
        NetImage(
            model = OkhttpUtils.getRandomImgLink(),
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(text = txt)


    }


}


@Composable
fun <E> DropDownSpinner(
    modifier: Modifier = Modifier,
    defaultText: String = "Select...",
    selectedItem: E,
    onItemSelected: (Int, E) -> Unit,
    itemList: List<E>?
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface)
        //.height(ELEMENT_HEIGHT)
        ,
        contentAlignment = Alignment.CenterStart
    ) {
        if (selectedItem == null || selectedItem.toString().isEmpty()) {
            Text(
                text = defaultText,
                modifier = Modifier.align(Alignment.Center)
                //.fillMaxWidth()
                //.padding(start = 16.dp, end = 16.dp, bottom = 3.dp),
                ,
                color = MaterialTheme.colors.onSurface.copy(.45f)
            )
        }

        Text(
            text = selectedItem?.toString() ?: "",
            modifier = Modifier.align(Alignment.Center)
            //.fillMaxWidth()
            //.padding(start = 16.dp, end = 32.dp, bottom = 3.dp),
            ,
            color = MaterialTheme.colors.onSurface
        )

        DropdownMenu(
            modifier = Modifier,
            expanded = isOpen,
            onDismissRequest = {
                isOpen = false
            },
        ) {
            itemList?.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        isOpen = false
                        onItemSelected(index, item)
                    }
                ) {
                    Text(item.toString())
                }
            }
        }


        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .clickable(
                    onClick = { isOpen = true }
                )
        )
    }
}