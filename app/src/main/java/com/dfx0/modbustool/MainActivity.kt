package com.dfx0.modbustool

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfx0.modbustool.utils.ModbusManager
import com.dfx0.modbustool.viewmodel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dfx0.modbustool.model.VarTag
import com.dfx0.modbustool.model.enums.VarType
import com.dfx0.modbustool.utils.PrefsUtil
import com.dfx0.modbustool.viewmodel.DBViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrawerApp()
        }
    }
}

var isAddVarTag = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun DrawerApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()
    val sharedViewModel: SharedViewModel = viewModel()
    val dbViewModel: DBViewModel = viewModel()
    val isConnectedPLC by sharedViewModel.isConnectedPLC.collectAsState()
    var selectedTab by remember { mutableStateOf("首页") }
    val tabs = if (isConnectedPLC) {
        listOf("首页", "写入测试", "读取测试","变量管理","操作界面","关于")
    } else {
        listOf("首页","变量管理","关于")
    }



    //initialize delegate function
    ModbusManager.logText = sharedViewModel::log

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                tabs.forEach { tab ->
                    NavigationDrawerItem(
                        label = { Text(tab) },
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ModBus Tool") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize()) {
                when (selectedTab) {
                    "首页" -> InitMainPage(dbViewModel,sharedViewModel)
                    "写入测试" -> ModbusTestScreen()
                    "读取测试" -> ModbusReadScreen()
                    "变量管理" -> VarTagManager(dbViewModel,sharedViewModel)
                    "操作界面" -> ShowVarTag(dbViewModel,sharedViewModel)
                    "关于" -> About()
                }
            }
        }
    }
}

@Preview
@Composable
fun About(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "关于我们",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "这是一款使用 Modbus 协议进行 PLC 通信的调试工具，适用于工程师调试：\n1. 支持连接指定PLC设备\n2. 支持连续读、批量读取指定地址\n3. 支持读取指定地址数据并自动转为指定格式\n4. 支持添加/管理需要操作的变量到界面上，并能实时读取PLC数据到屏幕上。\n5. 功能不断完善中...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "杭州正宇科技有限公司\n版本号：v1.0.0\n联系邮箱：dfx0@outlook.com\n联系电话: 17322209993",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "© 2025 杭州正宇科技有限公司 All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun InitMainPage(dbViewModel: DBViewModel,sharedViewModel: SharedViewModel){
    val context = LocalContext.current
    var log by remember {
        mutableStateOf("")
    }
    var ipAddress by remember {
        mutableStateOf("192.168.1.88")
    }
    if(PrefsUtil.getIpAddress(context)!="")
        ipAddress = PrefsUtil.getIpAddress(context)
    log = sharedViewModel.getLogText.collectAsState().value

    LaunchedEffect(Unit) {
        sharedViewModel.updateVarTagList(dbViewModel.getVarTagDao().getAll())
    }

    val scope = rememberCoroutineScope()


    OutlinedCard(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize(1f),
    ) {
        Column(modifier = Modifier.padding(12.dp)){
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = {
                    Text("地址",  modifier = Modifier.fillMaxWidth(1f),)
                }
            )

            OutlinedButton(onClick = {
                var connected = false
                scope.launch {
                    connected = ModbusManager.initTcp(ipAddress)
                    sharedViewModel.log(if (connected) "ModBus 连接成功" else "ModBus 连接失败")
                    if(connected)
                        PrefsUtil.saveIpAddress(context,ipAddress)
                    sharedViewModel.updateConnectedPLC(connected)
                    withContext (Dispatchers.Main){
                        Toast.makeText(context, if (connected) "ModBus 连接成功" else "ModBus 连接失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(text = "连接", modifier = Modifier.fillMaxWidth(1f))
            }

            OutlinedButton(onClick = {
                scope.launch {
                    sharedViewModel.clearLog()
                }
            }) {
                Text(text = "清空Log", modifier = Modifier.fillMaxWidth(1f))
            }

            OutlinedButton(onClick = {
                scope.launch {
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1000",
                        describe = "总产量",
                        dataType = VarType.INT32,
                        unit = "个"
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1002",
                        describe = "今日产量",
                        dataType = VarType.INT16,
                        unit = "个"
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1004",
                        describe = "生产速度",
                        dataType = VarType.REAL,
                        unit = "个"
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1006",
                        describe = "左移",
                        dataType = VarType.JoyBOOL,
                        trueText = "移动",
                        falseText = "停止",
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1008",
                        describe = "右移",
                        dataType = VarType.JoyBOOL,
                        trueText = "移动",
                        falseText = "停止",
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1009",
                        describe = "抓取",
                        dataType = VarType.JoyBOOL,
                        trueText = "抓取中",
                        falseText = "停止",
                    ))
                    dbViewModel.getVarTagDao().insert(VarTag(
                        tag = "%MW1010",
                        describe = "设备开关",
                        dataType = VarType.BOOL,
                    ))
                    sharedViewModel.log("初始化成功!")
                }
            }) {
                Text(text = "初始化测试数据", modifier = Modifier.fillMaxWidth(1f))
            }

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ){
                Text(
                    text = log,
                    modifier = Modifier.padding(12.dp)
                )
            }

        }
    }
}



@Preview
@Composable
fun ModbusTestScreen() {
    var address by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var readResult by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        while (true) {
            val r = ModbusManager.readMultipleHoldingRegisters(address.toIntOrNull(), 10)
            if (r != null && readResult != r.toString()) {
                readResult = r.toString()
            }
            kotlinx.coroutines.delay(100)
        }
    }

    Column(modifier = Modifier.padding(12.dp)) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("地址") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("值") },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val success = ModbusManager.writeHoldingRegister(address.toIntOrNull() ?: 0, value.toIntOrNull() ?: 0)
                Toast.makeText(context, if (success) "写入成功" else "写入失败", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("提交")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("写入结果[写入地址后10个数]: $readResult")
    }
}



@Preview
@Composable
fun ModbusReadScreen() {
    var address by remember { mutableStateOf("") }
    var readCount by remember { mutableStateOf("") }
    var readResult by remember { mutableStateOf("") }
    var isContinuous by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit){
        while(true){
            if(isContinuous){
                val r = ModbusManager.readMultipleHoldingRegisters(address.toIntOrNull(), readCount.toIntOrNull())
                if (r != null && readResult != r.toString()) {
                    readResult = r.toString()
                }
            }
            delay(100)
        }
    }

    Column(modifier = Modifier.padding(12.dp)) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("起始地址") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = readCount,
            onValueChange = { readCount = it },
            label = { Text("读取数量") },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row (modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically){
            Button(onClick = {
                scope.launch {
                    val r = ModbusManager.readMultipleHoldingRegisters(address.toIntOrNull(), readCount.toIntOrNull())
                    if (r != null && readResult != r.toString()) {
                        readResult = r.toString()
                    }
                }
            }) {
                Text("读取")
            }
            Checkbox(
                checked = isContinuous,
                onCheckedChange = {
                    isContinuous = it
                })
            Text(text = "连续读取",modifier = Modifier.padding(start = 2.dp), textAlign = TextAlign.Center)

        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("读取结果: $readResult")

        Spacer(modifier = Modifier.height(26.dp))
        var beginAddress by remember { mutableStateOf("") }
        var varType by remember { mutableStateOf(VarType.BOOL) }
        OutlinedTextField(
            value = beginAddress,
            onValueChange = { beginAddress = it },
            label = { Text("起始地址") },
            modifier = Modifier.fillMaxWidth(1f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        VarTypeDropdown(selectedType = varType, onTypeSelected = {
            varType = it
        })
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    ModbusManager.readModbusValue(beginAddress.toIntOrNull() ?: 0, varType)
                }
                if (r != null && readResult != r.toString()) {
                    readResult = r.toString()
                }
            }
        }) {
            Text("读取")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("读取结果: $readResult")
    }
}


/**
 * The composable is used to manage VarTags
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarTagManager(dbViewModel: DBViewModel,sharedViewModel:SharedViewModel){
    var varTags = sharedViewModel.getVarTag.collectAsState().value
    var varTag by remember { mutableStateOf(VarTag("")) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VarTag 管理") },
                actions = {
                    IconButton(onClick = {
                        showDialog = true
                        isAddVarTag = true
                        varTag = VarTag("")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn (
            contentPadding  = padding
        ){
            items(
                count = varTags.size,
                key = { index -> varTags[index].tag }, // 推荐提供 key，提高性能
                itemContent = { index ->
                    VarTagCard(varTags[index],
                        onDelete = {
                            CoroutineScope(Dispatchers.IO).launch{
                                dbViewModel.getVarTagDao().delete(varTags[index])
                                varTags = dbViewModel.getVarTagDao().getAll()
                                sharedViewModel.updateVarTagList(varTags)
                            }

                            true
                        },
                        onUpdate = {
                            varTag = it
                            showDialog = true
                            true
                        })
                }
            )
        }
    }

    // 弹出输入框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("变量管理") },
            text = {
                VarTagInputFormWithSave(varTag,onSave = {
                    CoroutineScope(Dispatchers.IO).launch {
                        dbViewModel.getVarTagDao().insert(it)
                        val updated = dbViewModel.getVarTagDao().getAll()
                        sharedViewModel.updateVarTagList(updated)
                        withContext(Dispatchers.Main) {
                            showDialog = false
                            isAddVarTag = false
                        }
                    }
                },
                onCancel = {
                    showDialog = false
                    isAddVarTag = false
                })
            },
            confirmButton = {

            },
            dismissButton = {

            }
        )
    }


}


/**
 * The composable is used to manage a VarTag, and returns callback functions when the VarTag is modified
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarTagCard(tag: VarTag, onDelete: ((VarTag) -> Boolean)? = null, onUpdate: ((VarTag) -> Boolean)? = null){
    var showDialog by remember {
        mutableStateOf(false)
    }
    //val tag = VarTag("123")
    Card (
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(10.dp)
            .background(Color.White),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = {
            showDialog = true
        }
    ){
        Row{
            Text("[${tag.tag}]  ${tag.describe} | [${tag.modBusAddress}]", color = Color.Red, modifier = Modifier.padding(5.dp), fontSize = 20.sp)

        }
        Row {
            Text("类型：${tag.dataType}", color = Color.Gray, modifier = Modifier
                .weight(1f)
                .padding(5.dp))
            Text("单位：${tag.unit}", color = Color.Gray, modifier = Modifier
                .weight(1f)
                .padding(5.dp))
        }
        Row{
            Text("真文本：${tag.trueText}", color = Color.Gray, modifier = Modifier
                .weight(1f)
                .padding(5.dp))
            Text("假文本：${tag.falseText}", color = Color.Gray, modifier = Modifier
                .weight(1f)
                .padding(5.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("变量管理") },
            text = {
                   Column {
                       Button(onClick = {
                           if (onDelete != null) {
                               if(onDelete(tag))
                                    showDialog = false
                           }
                       }) {
                           Text(modifier = Modifier
                               .weight(1f)
                               .align(Alignment.CenterVertically)
                               ,text = "删除"
                               , textAlign = TextAlign.Center)
                       }

                       Button(onClick = {
                           if (onUpdate != null) {
                               if (onUpdate(tag))
                                   showDialog = false
                           }
                       }) {
                           Text(modifier = Modifier
                               .weight(1f)
                               .align(Alignment.CenterVertically),text = "修改", textAlign = TextAlign.Center)
                       }

                       Button(onClick = {
                           showDialog = false
                       }) {
                           Text(modifier = Modifier
                               .weight(1f)
                               .align(Alignment.CenterVertically),text = "取消", textAlign = TextAlign.Center)
                       }
                   }
            },
            confirmButton = {

            },
            dismissButton = {

            }
        )
    }
}


/**
 * The composable is used to adding or updating the VarTags
 */
@Composable
fun VarTagInputFormWithSave(
    initialVarTag: VarTag? = null,
    onSave: (VarTag) -> Unit,
    onCancel: (() -> Unit)? = null // 可选的取消回调
) {
    var tag by remember { mutableStateOf(initialVarTag?.tag ?: "") }
    var dataType by remember { mutableStateOf(initialVarTag?.dataType ?: VarType.INT16) }
    var describe by remember { mutableStateOf(initialVarTag?.describe ?: "") }
    var trueText by remember { mutableStateOf(initialVarTag?.trueText ?: "") }
    var falseText by remember { mutableStateOf(initialVarTag?.falseText ?: "") }
    var unit by remember { mutableStateOf(initialVarTag?.unit ?: "") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ){
        OutlinedTextField(
            value = tag,
            onValueChange = { tag = it },
            label = { Text("地址") },
            enabled = isAddVarTag,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        VarTypeDropdown(
            selectedType = dataType,
            onTypeSelected = { dataType = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = describe,
            onValueChange = { describe = it },
            label = { Text("描述") }
        )
        Spacer(modifier = Modifier.height(8.dp))

      if(dataType == VarType.BOOL ||dataType == VarType.JoyBOOL){
          OutlinedTextField(
              value = trueText,
              onValueChange = { trueText = it },
              label = { Text("True文本") },
              singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
              value = falseText,
              onValueChange = { falseText = it },
              label = { Text("False文本") },
              singleLine = true
          )
          Spacer(modifier = Modifier.height(16.dp))
      }else{
          OutlinedTextField(
              value = unit,
              onValueChange = { unit = it },
              label = { Text("单位") },
              singleLine = true
          )
          Spacer(modifier = Modifier.height(16.dp))
      }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    // 点击保存时构造VarTag并回调
                    val varTag = VarTag(
                        tag = tag,
                        dataType = dataType,
                        describe = describe.takeIf { it.isNotBlank() },
                        trueText = trueText.takeIf { it.isNotBlank() },
                        falseText = falseText.takeIf { it.isNotBlank() },
                        unit = unit
                    )
                    onSave(varTag)
                },
                enabled = tag.isNotBlank() // tag不能为空才可保存
            ) {
                Text("保存")
            }

            if (onCancel != null) {
                OutlinedButton(onClick = onCancel) {
                    Text("取消")
                }
            }
        }
    }
}


/***
 * the onValueChange callback is used to write values to Modbus when the value is changed
 */
@Composable
fun VarTagValueEditor(
    varTag: VarTag,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "${varTag.describe ?: varTag.tag}：",
            modifier = Modifier.weight(1.5f),
            fontSize = 18.sp
        )

        if (varTag.dataType == VarType.BOOL) {
            val checked = value == "true"
            Switch(
                checked = checked,
                onCheckedChange = { isChecked ->
                    onValueChange(if (isChecked) "true" else "false")
                }
            )
        } else if(varTag.dataType == VarType.JoyBOOL){
            val checked = value == "true"
            PressGestureButton(varTag,checked,
                onPress = {
                    onValueChange("true")
                    //write to modbus
                    println("pressed")
                },
                onRelease = {
                    onValueChange("false")
                    //write to modbus
                    println("released")
                } ,
                onLongPress = {
                    println("long Press")
                })
        }else{
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .align(Alignment.CenterVertically),
                enabled = false,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp, // 设置字体大小
                    textAlign = TextAlign.Center // 设置文本居中
                )
            )

            Text(text = varTag.unit ?: "",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Center)
        }
    }
}


@Composable
fun ShowVarTag(dbViewModel: DBViewModel,sharedViewModel: SharedViewModel){
    val varTags = sharedViewModel.getVarTag.collectAsState().value
    //the varTagValues should come from ViewModel
    val varTagValues = sharedViewModel.getTagValueDic.collectAsState().value
    LaunchedEffect(Unit) {
        sharedViewModel.initializeVarTagValuesIfNeeded()
        delay(100)
        sharedViewModel.readVarTag()
    }

    val boolTags = varTags.filter { it.dataType == VarType.BOOL }
    val joyBoolTags = varTags.filter { it.dataType == VarType.JoyBOOL }
    val otherTags = varTags.filter { it.dataType != VarType.BOOL && it.dataType !=  VarType.JoyBOOL}
    LazyColumn {
        items(boolTags) { tag ->
            val value = varTagValues[tag.tag] ?: ""
            VarTagValueEditor(
                varTag = tag,
                value = value,
                onValueChange = { newValue ->
                    //write to Modbus
                    //varTagValues[tag.tag] = newValue
                    CoroutineScope(Dispatchers.IO).launch {
                        ModbusManager.writeModbusValue(tag,newValue)
                    }
                }
            )
        }
        items(joyBoolTags) { tag ->
            val value = varTagValues[tag.tag] ?: ""
             VarTagValueEditor(
                varTag = tag,
                value = value,
                onValueChange = { newValue ->
                    //write to Modbus
                    //varTagValues[tag.tag] = newValue
                   CoroutineScope(Dispatchers.IO).launch {
                       ModbusManager.writeModbusValue(tag,newValue)
                   }
                }
            )
        }
        items(otherTags) { tag ->
            val value = varTagValues[tag.tag] ?: ""

            VarTagValueEditor(
                varTag = tag,
                value = value,
                onValueChange = { newValue ->
                    //write to Modbus
                    //varTagValues[tag.tag] = newValue
                    CoroutineScope(Dispatchers.IO).launch {
                        ModbusManager.writeModbusValue(tag,newValue)
                    }
                }
            )
        }
    }

}


@Composable
fun VarTypeDropdown(
    selectedType: VarType,
    onTypeSelected: (VarType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box (Modifier.clickable {
        expanded = true
    }){
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .fillMaxWidth(1f)
                .background(Color.White, shape = RoundedCornerShape(4.dp))
                .clickable {
                    expanded = true
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(1f),
                text = "类型：" + selectedType.displayName,
                fontSize = 13.sp,
                color = Color.Black
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            VarType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PressGestureButton(
    varTag: VarTag,
    press : Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    isPressed = press
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(40.dp)
            .background(if (isPressed) Color.Red else Color.LightGray)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        isPressed = true
                        tryAwaitRelease() // 等待释放事件
                        onRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = if(isPressed) varTag.trueText ?: (varTag.describe ?: varTag.tag) else varTag.falseText ?: (varTag.describe ?: varTag.tag),
            fontSize = 20.sp,
            color = if(isPressed) Color.White else Color.Black,
            textAlign = TextAlign.Center)
    }
}