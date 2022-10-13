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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ag2s.cronet.test.ui.theme.NetImage

private val ELEMENT_HEIGHT = 48.dp

@Composable
fun Editor(navController: NavController, screen: Screen, scaffoldState: ScaffoldState) {
    var txt by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {


        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Request Method", modifier = Modifier.weight(1.0f))


            DropDownSpinner(
                selectedItem = RequestState.httpMethod.value,
                onItemSelected = { _, item -> RequestState.httpMethod.value = item },
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
                selectedItem = RequestState.protocol.value,
                onItemSelected = { _, item -> RequestState.protocol.value = item },
                itemList = RequestState.protocols
            )
            TextField(
                modifier = Modifier.weight(1.0f),
                value = RequestState.url.value,
                onValueChange = { view -> RequestState.url.value = view })
        }
        NetImage(
            model = OkhttpUtils.getRandomImgLink(),
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(text = txt)

        LaunchedEffect(key1 = Unit) {
            txt = withContext(Dispatchers.IO) { OkhttpUtils.httpGet("https://http3.is") }

        }
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