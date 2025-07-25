package com.jiyingcao.a51fengliu.util

data class City(val code: String, val name: String)

// 一级列表（省级）的数据，不可变的 List
val provinceList: List<City> = listOf(
    City("110000", "北京市"),
    City("120000", "天津市"),
    City("130000", "河北省"),
    City("140000", "山西省"),
    City("150000", "内蒙古自治区"),
    City("210000", "辽宁省"),
    City("220000", "吉林省"),
    City("230000", "黑龙江省"),
    City("310000", "上海市"),
    City("320000", "江苏省"),
    City("330000", "浙江省"),
    City("340000", "安徽省"),
    City("350000", "福建省"),
    City("360000", "江西省"),
    City("370000", "山东省"),
    City("410000", "河南省"),
    City("420000", "湖北省"),
    City("430000", "湖南省"),
    City("440000", "广东省"),
    City("450000", "广西壮族自治区"),
    City("460000", "海南省"),
    City("500000", "重庆市"),
    City("510000", "四川省"),
    City("520000", "贵州省"),
    City("530000", "云南省"),
    City("540000", "西藏自治区"),
    City("610000", "陕西省"),
    City("620000", "甘肃省"),
    City("630000", "青海省"),
    City("640000", "宁夏回族自治区"),
    City("650000", "新疆维吾尔自治区"),
    City("710000", "台湾省"),
    City("810000", "香港特别行政区"),
    City("820000", "澳门特别行政区"),
    // ... 其他省份
)

val beijing by lazy {
    listOf(
        City("110000", "北京市"),
        City("110101", "东城区"),
        City("110102", "西城区"),
        City("110105", "朝阳区"),
        City("110106", "丰台区"),
        City("110107", "石景山区"),
        City("110108", "海淀区"),
        City("110109", "门头沟区"),
        City("110111", "房山区"),
        City("110112", "通州区"),
        City("110113", "顺义区"),
        City("110114", "昌平区"),
        City("110115", "大兴区"),
        City("110116", "怀柔区"),
        City("110117", "平谷区"),
        City("110118", "密云区"),
        City("110119", "延庆区"),
    )
}

val tianjin by lazy {
    listOf(
        City("120000", "天津市"),
        City("120101", "和平区"),
        City("120102", "河东区"),
        City("120103", "河西区"),
        City("120104", "南开区"),
        City("120105", "河北区"),
        City("120106", "红桥区"),
        City("120110", "东丽区"),
        City("120111", "西青区"),
        City("120112", "津南区"),
        City("120113", "北辰区"),
        City("120114", "武清区"),
        City("120115", "宝坻区"),
        City("120116", "滨海新区"),
        City("120117", "宁河区"),
        City("120118", "静海区"),
        City("120119", "蓟州区"),
    )
}

val hebei by lazy {
    listOf(
        City("130000", "河北省"),
        City("130100", "石家庄市"),
        City("130200", "唐山市"),
        City("130300", "秦皇岛市"),
        City("130400", "邯郸市"),
        City("130500", "邢台市"),
        City("130600", "保定市"),
        City("130700", "张家口市"),
        City("130800", "承德市"),
        City("130900", "沧州市"),
        City("131000", "廊坊市"),
        City("131100", "衡水市"),
    )
}

val shanxi by lazy {
    listOf(
        City("140000", "山西省"),
        City("140100", "太原市"),
        City("140200", "大同市"),
        City("140300", "阳泉市"),
        City("140400", "长治市"),
        City("140500", "晋城市"),
        City("140600", "朔州市"),
        City("140700", "晋中市"),
        City("140800", "运城市"),
        City("140900", "忻州市"),
        City("141000", "临汾市"),
        City("141100", "吕梁市"),
    )
}

val neimenggu by lazy {
    listOf(
        City("150000", "内蒙古自治区"),
        City("150100", "呼和浩特市"),
        City("150200", "包头市"),
        City("150300", "乌海市"),
        City("150400", "赤峰市"),
        City("150500", "通辽市"),
        City("150600", "鄂尔多斯市"),
        City("150700", "呼伦贝尔市"),
        City("150800", "巴彦淖尔市"),
        City("150900", "乌兰察布市"),
        City("152200", "兴安盟"),
        City("152500", "锡林郭勒盟"),
        City("152900", "阿拉善盟"),
    )
}

val liaoning by lazy {
    listOf(
        City("210000", "辽宁省"),
        City("210100", "沈阳市"),
        City("210200", "大连市"),
        City("210300", "鞍山市"),
        City("210400", "抚顺市"),
        City("210500", "本溪市"),
        City("210600", "丹东市"),
        City("210700", "锦州市"),
        City("210800", "营口市"),
        City("210900", "阜新市"),
        City("211000", "辽阳市"),
        City("211100", "盘锦市"),
        City("211200", "铁岭市"),
        City("211300", "朝阳市"),
        City("211400", "葫芦岛市"),
    )
}

val jilin by lazy {
    listOf(
        City("220000", "吉林省"),
        City("220100", "长春市"),
        City("220200", "吉林市"),
        City("220300", "四平市"),
        City("220400", "辽源市"),
        City("220500", "通化市"),
        City("220600", "白山市"),
        City("220700", "松原市"),
        City("220800", "白城市"),
        City("222400", "延边朝鲜族自治州"),
    )
}

val heilongjiang by lazy {
    listOf(
        City("230000", "黑龙江省"),
        City("230100", "哈尔滨市"),
        City("230200", "齐齐哈尔市"),
        City("230300", "鸡西市"),
        City("230400", "鹤岗市"),
        City("230500", "双鸭山市"),
        City("230600", "大庆市"),
        City("230700", "伊春市"),
        City("230800", "佳木斯市"),
        City("230900", "七台河市"),
        City("231000", "牡丹江市"),
        City("231100", "黑河市"),
        City("231200", "绥化市"),
        City("232700", "大兴安岭地区"),
    )
}

val shanghai by lazy {
    listOf(
        City("310000", "上海市"),
        City("310101", "黄浦区"),
        City("310104", "徐汇区"),
        City("310105", "长宁区"),
        City("310106", "静安区"),
        City("310107", "普陀区"),
        City("310109", "虹口区"),
        City("310110", "杨浦区"),
        City("310112", "闵行区"),
        City("310113", "宝山区"),
        City("310114", "嘉定区"),
        City("310115", "浦东新区"),
        City("310116", "金山区"),
        City("310117", "松江区"),
        City("310118", "青浦区"),
        City("310120", "奉贤区"),
        City("310151", "崇明区"),
    )
}

val jiangsu by lazy {
    listOf(
        City("320000", "江苏省"),
        City("320100", "南京市"),
        City("320200", "无锡市"),
        City("320300", "徐州市"),
        City("320400", "常州市"),
        City("320500", "苏州市"),
        City("320600", "南通市"),
        City("320700", "连云港市"),
        City("320800", "淮安市"),
        City("320900", "盐城市"),
        City("321000", "扬州市"),
        City("321100", "镇江市"),
        City("321200", "泰州市"),
        City("321300", "宿迁市"),
    )
}

val zhejiang by lazy {
    listOf(
        City("330000", "浙江省"),
        City("330100", "杭州市"),
        City("330200", "宁波市"),
        City("330300", "温州市"),
        City("330400", "嘉兴市"),
        City("330500", "湖州市"),
        City("330600", "绍兴市"),
        City("330700", "金华市"),
        City("330800", "衢州市"),
        City("330900", "舟山市"),
        City("331000", "台州市"),
        City("331100", "丽水市"),
    )
}

val anhui by lazy {
    listOf(
        City("340000", "安徽省"),
        City("340100", "合肥市"),
        City("340200", "芜湖市"),
        City("340300", "蚌埠市"),
        City("340400", "淮南市"),
        City("340500", "马鞍山市"),
        City("340600", "淮北市"),
        City("340700", "铜陵市"),
        City("340800", "安庆市"),
        City("341000", "黄山市"),
        City("341100", "滁州市"),
        City("341200", "阜阳市"),
        City("341300", "宿州市"),
        City("341500", "六安市"),
        City("341600", "亳州市"),
        City("341700", "池州市"),
        City("341800", "宣城市"),
    )
}

val fujian by lazy {
    listOf(
        City("350000", "福建省"),
        City("350100", "福州市"),
        City("350200", "厦门市"),
        City("350300", "莆田市"),
        City("350400", "三明市"),
        City("350500", "泉州市"),
        City("350600", "漳州市"),
        City("350700", "南平市"),
        City("350800", "龙岩市"),
        City("350900", "宁德市"),
    )
}

val jiangxi by lazy {
    listOf(
        City("360000", "江西省"),
        City("360100", "南昌市"),
        City("360200", "景德镇市"),
        City("360300", "萍乡市"),
        City("360400", "九江市"),
        City("360500", "新余市"),
        City("360600", "鹰潭市"),
        City("360700", "赣州市"),
        City("360800", "吉安市"),
        City("360900", "宜春市"),
        City("361000", "抚州市"),
        City("361100", "上饶市"),
    )
}

val shandong by lazy {
    listOf(
        City("370000", "山东省"),
        City("370100", "济南市"),
        City("370200", "青岛市"),
        City("370300", "淄博市"),
        City("370400", "枣庄市"),
        City("370500", "东营市"),
        City("370600", "烟台市"),
        City("370700", "潍坊市"),
        City("370800", "济宁市"),
        City("370900", "泰安市"),
        City("371000", "威海市"),
        City("371100", "日照市"),
        City("371300", "临沂市"),
        City("371400", "德州市"),
        City("371500", "聊城市"),
        City("371600", "滨州市"),
        City("371700", "菏泽市"),
    )
}

val henan by lazy {
    listOf(
        City("410000", "河南省"),
        City("410100", "郑州市"),
        City("410200", "开封市"),
        City("410300", "洛阳市"),
        City("410400", "平顶山市"),
        City("410500", "安阳市"),
        City("410600", "鹤壁市"),
        City("410700", "新乡市"),
        City("410800", "焦作市"),
        City("410900", "濮阳市"),
        City("411000", "许昌市"),
        City("411100", "漯河市"),
        City("411200", "三门峡市"),
        City("411300", "南阳市"),
        City("411400", "商丘市"),
        City("411500", "信阳市"),
        City("411600", "周口市"),
        City("411700", "驻马店市"),
        City("419001", "济源市"),
    )
}

val hubei by lazy {
    listOf(
        City("420000", "湖北省"),
        City("420100", "武汉市"),
        City("420200", "黄石市"),
        City("420300", "十堰市"),
        City("420500", "宜昌市"),
        City("420600", "襄阳市"),
        City("420700", "鄂州市"),
        City("420800", "荆门市"),
        City("420900", "孝感市"),
        City("421000", "荆州市"),
        City("421100", "黄冈市"),
        City("421200", "咸宁市"),
        City("421300", "随州市"),
        City("422800", "恩施土家族苗族自治州"),
        City("429004", "仙桃市"),
        City("429005", "潜江市"),
        City("429006", "天门市"),
        City("429021", "神农架林区"),
    )
}

val hunan by lazy {
    listOf(
        City("430000", "湖南省"),
        City("430100", "长沙市"),
        City("430200", "株洲市"),
        City("430300", "湘潭市"),
        City("430400", "衡阳市"),
        City("430500", "邵阳市"),
        City("430600", "岳阳市"),
        City("430700", "常德市"),
        City("430800", "张家界市"),
        City("430900", "益阳市"),
        City("431000", "郴州市"),
        City("431100", "永州市"),
        City("431200", "怀化市"),
        City("431300", "娄底市"),
        City("433100", "湘西土家族苗族自治州"),
    )
}

val guangdong by lazy {
    listOf(
        City("440000", "广东省"),
        City("440100", "广州市"),
        City("440200", "韶关市"),
        City("440300", "深圳市"),
        City("440400", "珠海市"),
        City("440500", "汕头市"),
        City("440600", "佛山市"),
        City("440700", "江门市"),
        City("440800", "湛江市"),
        City("440900", "茂名市"),
        City("441200", "肇庆市"),
        City("441300", "惠州市"),
        City("441400", "梅州市"),
        City("441500", "汕尾市"),
        City("441600", "河源市"),
        City("441700", "阳江市"),
        City("441800", "清远市"),
        City("441900", "东莞市"),
        City("442000", "中山市"),
        City("445100", "潮州市"),
        City("445200", "揭阳市"),
        City("445300", "云浮市"),
    )
}

val guangxi by lazy {
    listOf(
        City("450000", "广西壮族自治区"),
        City("450100", "南宁市"),
        City("450200", "柳州市"),
        City("450300", "桂林市"),
        City("450400", "梧州市"),
        City("450500", "北海市"),
        City("450600", "防城港市"),
        City("450700", "钦州市"),
        City("450800", "贵港市"),
        City("450900", "玉林市"),
        City("451000", "百色市"),
        City("451100", "贺州市"),
        City("451200", "河池市"),
        City("451300", "来宾市"),
        City("451400", "崇左市"),
    )
}

val hainan by lazy {
    listOf(
        City("460000", "海南省"),
        City("460100", "海口市"),
        City("460200", "三亚市"),
        City("460300", "三沙市"),
        City("460400", "儋州市"),
        City("469001", "五指山市"),
        City("469002", "琼海市"),
        City("469005", "文昌市"),
        City("469006", "万宁市"),
        City("469007", "东方市"),
        City("469021", "定安县"),
        City("469022", "屯昌县"),
        City("469023", "澄迈县"),
        City("469024", "临高县"),
        City("469025", "白沙黎族自治县"),
        City("469026", "昌江黎族自治县"),
        City("469027", "乐东黎族自治县"),
        City("469028", "陵水黎族自治县"),
        City("469029", "保亭黎族苗族自治县"),
        City("469030", "琼中黎族苗族自治县"),
    )
}

val chongqing by lazy {
    listOf(
        City("500000", "重庆市"),
        City("500101", "万州区"),
        City("500102", "涪陵区"),
        City("500103", "渝中区"),
        City("500104", "大渡口区"),
        City("500105", "江北区"),
        City("500106", "沙坪坝区"),
        City("500107", "九龙坡区"),
        City("500108", "南岸区"),
        City("500109", "北碚区"),
        City("500110", "綦江区"),
        City("500111", "大足区"),
        City("500112", "渝北区"),
        City("500113", "巴南区"),
        City("500114", "黔江区"),
        City("500115", "长寿区"),
        City("500116", "江津区"),
        City("500117", "合川区"),
        City("500118", "永川区"),
        City("500119", "南川区"),
        City("500120", "璧山区"),
        City("500151", "铜梁区"),
        City("500152", "潼南区"),
        City("500153", "荣昌区"),
        City("500154", "开州区"),
        City("500155", "梁平区"),
        City("500156", "武隆区"),
        City("500229", "城口县"),
        City("500230", "丰都县"),
        City("500231", "垫江县"),
        City("500233", "忠县"),
        City("500235", "云阳县"),
        City("500236", "奉节县"),
        City("500237", "巫山县"),
        City("500238", "巫溪县"),
        City("500240", "石柱土家族自治县"),
        City("500241", "秀山土家族苗族自治县"),
        City("500242", "酉阳土家族苗族自治县"),
        City("500243", "彭水苗族土家族自治县"),
    )
}

val sichuan by lazy {
    listOf(
        City("510000", "四川省"),
        City("510100", "成都市"),
        City("510300", "自贡市"),
        City("510400", "攀枝花市"),
        City("510500", "泸州市"),
        City("510600", "德阳市"),
        City("510700", "绵阳市"),
        City("510800", "广元市"),
        City("510900", "遂宁市"),
        City("511000", "内江市"),
        City("511100", "乐山市"),
        City("511300", "南充市"),
        City("511400", "眉山市"),
        City("511500", "宜宾市"),
        City("511600", "广安市"),
        City("511700", "达州市"),
        City("511800", "雅安市"),
        City("511900", "巴中市"),
        City("512000", "资阳市"),
        City("513200", "阿坝藏族羌族自治州"),
        City("513300", "甘孜藏族自治州"),
        City("513400", "凉山彝族自治州"),
    )
}

val guizhou by lazy {
    listOf(
        City("520000", "贵州省"),
        City("520100", "贵阳市"),
        City("520200", "六盘水市"),
        City("520300", "遵义市"),
        City("520400", "安顺市"),
        City("520500", "毕节市"),
        City("520600", "铜仁市"),
        City("522300", "黔西南布依族苗族自治州"),
        City("522600", "黔东南苗族侗族自治州"),
        City("522700", "黔南布依族苗族自治州"),
    )
}

val yunnan by lazy {
    listOf(
        City("530000", "云南省"),
        City("530100", "昆明市"),
        City("530300", "曲靖市"),
        City("530400", "玉溪市"),
        City("530500", "保山市"),
        City("530600", "昭通市"),
        City("530700", "丽江市"),
        City("530800", "普洱市"),
        City("530900", "临沧市"),
        City("532300", "楚雄彝族自治州"),
        City("532500", "红河哈尼族彝族自治州"),
        City("532600", "文山壮族苗族自治州"),
        City("532800", "西双版纳傣族自治州"),
        City("532900", "大理白族自治州"),
        City("533100", "德宏傣族景颇族自治州"),
        City("533300", "怒江傈僳族自治州"),
        City("533400", "迪庆藏族自治州"),
    )
}

val xizang by lazy {
    listOf(
        City("540000", "西藏自治区"),
        City("540100", "拉萨市"),
        City("540200", "日喀则市"),
        City("540300", "昌都市"),
        City("540400", "林芝市"),
        City("540500", "山南市"),
        City("540600", "那曲市"),
        City("542500", "阿里地区"),
    )
}

val shaanxi by lazy {
    listOf(
        City("610000", "陕西省"),
        City("610100", "西安市"),
        City("610200", "铜川市"),
        City("610300", "宝鸡市"),
        City("610400", "咸阳市"),
        City("610500", "渭南市"),
        City("610600", "延安市"),
        City("610700", "汉中市"),
        City("610800", "榆林市"),
        City("610900", "安康市"),
        City("611000", "商洛市"),
    )
}

val gansu by lazy {
    listOf(
        City("620000", "甘肃省"),
        City("620100", "兰州市"),
        City("620200", "嘉峪关市"),
        City("620300", "金昌市"),
        City("620400", "白银市"),
        City("620500", "天水市"),
        City("620600", "武威市"),
        City("620700", "张掖市"),
        City("620800", "平凉市"),
        City("620900", "酒泉市"),
        City("621000", "庆阳市"),
        City("621100", "定西市"),
        City("621200", "陇南市"),
        City("622900", "临夏回族自治州"),
        City("623000", "甘南藏族自治州"),
    )
}

val qinghai by lazy {
    listOf(
        City("630000", "青海省"),
        City("630100", "西宁市"),
        City("630200", "海东市"),
        City("632200", "海北藏族自治州"),
        City("632300", "黄南藏族自治州"),
        City("632500", "海南藏族自治州"),
        City("632600", "果洛藏族自治州"),
        City("632700", "玉树藏族自治州"),
        City("632800", "海西蒙古族藏族自治州"),
    )
}

val ningxia by lazy {
    listOf(
        City("640000", "宁夏回族自治区"),
        City("640100", "银川市"),
        City("640200", "石嘴山市"),
        City("640300", "吴忠市"),
        City("640400", "固原市"),
        City("640500", "中卫市"),
    )
}

val xinjiang by lazy {
    listOf(
        City("650000", "新疆维吾尔自治区"),
        City("650100", "乌鲁木齐市"),
        City("650200", "克拉玛依市"),
        City("650400", "吐鲁番市"),
        City("650500", "哈密市"),
        City("652300", "昌吉回族自治州"),
        City("652700", "博尔塔拉蒙古自治州"),
        City("652800", "巴音郭楞蒙古自治州"),
        City("652900", "阿克苏地区"),
        City("653000", "克孜勒苏柯尔克孜自治州"),
        City("653100", "喀什地区"),
        City("653200", "和田地区"),
        City("654000", "伊犁哈萨克自治州"),
        City("654200", "塔城地区"),
        City("654300", "阿勒泰地区"),
        City("659001", "石河子市"),
        City("659002", "阿拉尔市"),
        City("659003", "图木舒克市"),
        City("659004", "五家渠市"),
        City("659005", "北屯市"),
        City("659006", "铁门关市"),
        City("659007", "双河市"),
        City("659008", "可克达拉市"),
        City("659009", "昆玉市"),
    )
}

/**
 * 根据省份获取城市列表
 */
fun getCitiesForProvince(provinceCode: String): List<City> =
    when (provinceCode) {
        "110000" -> beijing
        "120000" -> tianjin
        "130000" -> hebei
        "140000" -> shanxi
        "150000" -> neimenggu
        "210000" -> liaoning
        "220000" -> jilin
        "230000" -> heilongjiang
        "310000" -> shanghai
        "320000" -> jiangsu
        "330000" -> zhejiang
        "340000" -> anhui
        "350000" -> fujian
        "360000" -> jiangxi
        "370000" -> shandong
        "410000" -> henan
        "420000" -> hubei
        "430000" -> hunan
        "440000" -> guangdong
        "450000" -> guangxi
        "460000" -> hainan
        "500000" -> chongqing
        "510000" -> sichuan
        "520000" -> guizhou
        "530000" -> yunnan
        "540000" -> xizang
        "610000" -> shaanxi
        "620000" -> gansu
        "630000" -> qinghai
        "640000" -> ningxia
        "650000" -> xinjiang
        else -> emptyList()
    }

/** 行政区划代码 to 行政区划名称 */
val administrativeDivisions = mapOf( // TODO 移除省直辖县，如：429004 仙桃市
    "110000" to "北京市",
    "110101" to "东城区",
    "110102" to "西城区",
    "110105" to "朝阳区",
    "110106" to "丰台区",
    "110107" to "石景山区",
    "110108" to "海淀区",
    "110109" to "门头沟区",
    "110111" to "房山区",
    "110112" to "通州区",
    "110113" to "顺义区",
    "110114" to "昌平区",
    "110115" to "大兴区",
    "110116" to "怀柔区",
    "110117" to "平谷区",
    "110118" to "密云区",
    "110119" to "延庆区",

    "120000" to "天津市",
    "120101" to "和平区",
    "120102" to "河东区",
    "120103" to "河西区",
    "120104" to "南开区",
    "120105" to "河北区",
    "120106" to "红桥区",
    "120110" to "东丽区",
    "120111" to "西青区",
    "120112" to "津南区",
    "120113" to "北辰区",
    "120114" to "武清区",
    "120115" to "宝坻区",
    "120116" to "滨海新区",
    "120117" to "宁河区",
    "120118" to "静海区",
    "120119" to "蓟州区",

    "130000" to "河北省",
    "130100" to "石家庄市",
    "130200" to "唐山市",
    "130300" to "秦皇岛市",
    "130400" to "邯郸市",
    "130500" to "邢台市",
    "130600" to "保定市",
    "130700" to "张家口市",
    "130800" to "承德市",
    "130900" to "沧州市",
    "131000" to "廊坊市",
    "131100" to "衡水市",

    "140000" to "山西省",
    "140100" to "太原市",
    "140200" to "大同市",
    "140300" to "阳泉市",
    "140400" to "长治市",
    "140500" to "晋城市",
    "140600" to "朔州市",
    "140700" to "晋中市",
    "140800" to "运城市",
    "140900" to "忻州市",
    "141000" to "临汾市",
    "141100" to "吕梁市",

    "150000" to "内蒙古自治区",
    "150100" to "呼和浩特市",
    "150200" to "包头市",
    "150300" to "乌海市",
    "150400" to "赤峰市",
    "150500" to "通辽市",
    "150600" to "鄂尔多斯市",
    "150700" to "呼伦贝尔市",
    "150800" to "巴彦淖尔市",
    "150900" to "乌兰察布市",
    "152200" to "兴安盟",
    "152500" to "锡林郭勒盟",
    "152900" to "阿拉善盟",

    "210000" to "辽宁省",
    "210100" to "沈阳市",
    "210200" to "大连市",
    "210300" to "鞍山市",
    "210400" to "抚顺市",
    "210500" to "本溪市",
    "210600" to "丹东市",
    "210700" to "锦州市",
    "210800" to "营口市",
    "210900" to "阜新市",
    "211000" to "辽阳市",
    "211100" to "盘锦市",
    "211200" to "铁岭市",
    "211300" to "朝阳市",
    "211400" to "葫芦岛市",

    "220000" to "吉林省",
    "220100" to "长春市",
    "220200" to "吉林市",
    "220300" to "四平市",
    "220400" to "辽源市",
    "220500" to "通化市",
    "220600" to "白山市",
    "220700" to "松原市",
    "220800" to "白城市",
    "222400" to "延边朝鲜族自治州",

    "230000" to "黑龙江省",
    "230100" to "哈尔滨市",
    "230200" to "齐齐哈尔市",
    "230300" to "鸡西市",
    "230400" to "鹤岗市",
    "230500" to "双鸭山市",
    "230600" to "大庆市",
    "230700" to "伊春市",
    "230800" to "佳木斯市",
    "230900" to "七台河市",
    "231000" to "牡丹江市",
    "231100" to "黑河市",
    "231200" to "绥化市",
    "232700" to "大兴安岭地区",

    "310000" to "上海市",
    "310101" to "黄浦区",
    "310104" to "徐汇区",
    "310105" to "长宁区",
    "310106" to "静安区",
    "310107" to "普陀区",
    "310109" to "虹口区",
    "310110" to "杨浦区",
    "310112" to "闵行区",
    "310113" to "宝山区",
    "310114" to "嘉定区",
    "310115" to "浦东新区",
    "310116" to "金山区",
    "310117" to "松江区",
    "310118" to "青浦区",
    "310120" to "奉贤区",
    "310151" to "崇明区",

    "320000" to "江苏省",
    "320100" to "南京市",
    "320200" to "无锡市",
    "320300" to "徐州市",
    "320400" to "常州市",
    "320500" to "苏州市",
    "320600" to "南通市",
    "320700" to "连云港市",
    "320800" to "淮安市",
    "320900" to "盐城市",
    "321000" to "扬州市",
    "321100" to "镇江市",
    "321200" to "泰州市",
    "321300" to "宿迁市",

    "330000" to "浙江省",
    "330100" to "杭州市",
    "330200" to "宁波市",
    "330300" to "温州市",
    "330400" to "嘉兴市",
    "330500" to "湖州市",
    "330600" to "绍兴市",
    "330700" to "金华市",
    "330800" to "衢州市",
    "330900" to "舟山市",
    "331000" to "台州市",
    "331100" to "丽水市",

    "340000" to "安徽省",
    "340100" to "合肥市",
    "340200" to "芜湖市",
    "340300" to "蚌埠市",
    "340400" to "淮南市",
    "340500" to "马鞍山市",
    "340600" to "淮北市",
    "340700" to "铜陵市",
    "340800" to "安庆市",
    "341000" to "黄山市",
    "341100" to "滁州市",
    "341200" to "阜阳市",
    "341300" to "宿州市",
    "341500" to "六安市",
    "341600" to "亳州市",
    "341700" to "池州市",
    "341800" to "宣城市",

    "350000" to "福建省",
    "350100" to "福州市",
    "350200" to "厦门市",
    "350300" to "莆田市",
    "350400" to "三明市",
    "350500" to "泉州市",
    "350600" to "漳州市",
    "350700" to "南平市",
    "350800" to "龙岩市",
    "350900" to "宁德市",

    "360000" to "江西省",
    "360100" to "南昌市",
    "360200" to "景德镇市",
    "360300" to "萍乡市",
    "360400" to "九江市",
    "360500" to "新余市",
    "360600" to "鹰潭市",
    "360700" to "赣州市",
    "360800" to "吉安市",
    "360900" to "宜春市",
    "361000" to "抚州市",
    "361100" to "上饶市",

    "370000" to "山东省",
    "370100" to "济南市",
    "370200" to "青岛市",
    "370300" to "淄博市",
    "370400" to "枣庄市",
    "370500" to "东营市",
    "370600" to "烟台市",
    "370700" to "潍坊市",
    "370800" to "济宁市",
    "370900" to "泰安市",
    "371000" to "威海市",
    "371100" to "日照市",
    "371300" to "临沂市",
    "371400" to "德州市",
    "371500" to "聊城市",
    "371600" to "滨州市",
    "371700" to "菏泽市",

    "410000" to "河南省",
    "410100" to "郑州市",
    "410200" to "开封市",
    "410300" to "洛阳市",
    "410400" to "平顶山市",
    "410500" to "安阳市",
    "410600" to "鹤壁市",
    "410700" to "新乡市",
    "410800" to "焦作市",
    "410900" to "濮阳市",
    "411000" to "许昌市",
    "411100" to "漯河市",
    "411200" to "三门峡市",
    "411300" to "南阳市",
    "411400" to "商丘市",
    "411500" to "信阳市",
    "411600" to "周口市",
    "411700" to "驻马店市",
    "419001" to "济源市",

    "420000" to "湖北省",
    "420100" to "武汉市",
    "420200" to "黄石市",
    "420300" to "十堰市",
    "420500" to "宜昌市",
    "420600" to "襄阳市",
    "420700" to "鄂州市",
    "420800" to "荆门市",
    "420900" to "孝感市",
    "421000" to "荆州市",
    "421100" to "黄冈市",
    "421200" to "咸宁市",
    "421300" to "随州市",
    "422800" to "恩施土家族苗族自治州",
    "429004" to "仙桃市",
    "429005" to "潜江市",
    "429006" to "天门市",
    "429021" to "神农架林区",

    "430000" to "湖南省",
    "430100" to "长沙市",
    "430200" to "株洲市",
    "430300" to "湘潭市",
    "430400" to "衡阳市",
    "430500" to "邵阳市",
    "430600" to "岳阳市",
    "430700" to "常德市",
    "430800" to "张家界市",
    "430900" to "益阳市",
    "431000" to "郴州市",
    "431100" to "永州市",
    "431200" to "怀化市",
    "431300" to "娄底市",
    "433100" to "湘西土家族苗族自治州",

    "440000" to "广东省",
    "440100" to "广州市",
    "440200" to "韶关市",
    "440300" to "深圳市",
    "440400" to "珠海市",
    "440500" to "汕头市",
    "440600" to "佛山市",
    "440700" to "江门市",
    "440800" to "湛江市",
    "440900" to "茂名市",
    "441200" to "肇庆市",
    "441300" to "惠州市",
    "441400" to "梅州市",
    "441500" to "汕尾市",
    "441600" to "河源市",
    "441700" to "阳江市",
    "441800" to "清远市",
    "441900" to "东莞市",
    "442000" to "中山市",
    "445100" to "潮州市",
    "445200" to "揭阳市",
    "445300" to "云浮市",

    "450000" to "广西壮族自治区",
    "450100" to "南宁市",
    "450200" to "柳州市",
    "450300" to "桂林市",
    "450400" to "梧州市",
    "450500" to "北海市",
    "450600" to "防城港市",
    "450700" to "钦州市",
    "450800" to "贵港市",
    "450900" to "玉林市",
    "451000" to "百色市",
    "451100" to "贺州市",
    "451200" to "河池市",
    "451300" to "来宾市",
    "451400" to "崇左市",

    "460000" to "海南省",
    "460100" to "海口市",
    "460200" to "三亚市",
    "460300" to "三沙市",
    "460400" to "儋州市",
    "469001" to "五指山市",
    "469002" to "琼海市",
    "469005" to "文昌市",
    "469006" to "万宁市",
    "469007" to "东方市",
    "469021" to "定安县",
    "469022" to "屯昌县",
    "469023" to "澄迈县",
    "469024" to "临高县",
    "469025" to "白沙黎族自治县",
    "469026" to "昌江黎族自治县",
    "469027" to "乐东黎族自治县",
    "469028" to "陵水黎族自治县",
    "469029" to "保亭黎族苗族自治县",
    "469030" to "琼中黎族苗族自治县",

    "500000" to "重庆市",
    "500101" to "万州区",
    "500102" to "涪陵区",
    "500103" to "渝中区",
    "500104" to "大渡口区",
    "500105" to "江北区",
    "500106" to "沙坪坝区",
    "500107" to "九龙坡区",
    "500108" to "南岸区",
    "500109" to "北碚区",
    "500110" to "綦江区",
    "500111" to "大足区",
    "500112" to "渝北区",
    "500113" to "巴南区",
    "500114" to "黔江区",
    "500115" to "长寿区",
    "500116" to "江津区",
    "500117" to "合川区",
    "500118" to "永川区",
    "500119" to "南川区",
    "500120" to "璧山区",
    "500151" to "铜梁区",
    "500152" to "潼南区",
    "500153" to "荣昌区",
    "500154" to "开州区",
    "500155" to "梁平区",
    "500156" to "武隆区",
    "500229" to "城口县",
    "500230" to "丰都县",
    "500231" to "垫江县",
    "500233" to "忠县",
    "500235" to "云阳县",
    "500236" to "奉节县",
    "500237" to "巫山县",
    "500238" to "巫溪县",
    "500240" to "石柱土家族自治县",
    "500241" to "秀山土家族苗族自治县",
    "500242" to "酉阳土家族苗族自治县",
    "500243" to "彭水苗族土家族自治县",

    "510000" to "四川省",
    "510100" to "成都市",
    "510300" to "自贡市",
    "510400" to "攀枝花市",
    "510500" to "泸州市",
    "510600" to "德阳市",
    "510700" to "绵阳市",
    "510800" to "广元市",
    "510900" to "遂宁市",
    "511000" to "内江市",
    "511100" to "乐山市",
    "511300" to "南充市",
    "511400" to "眉山市",
    "511500" to "宜宾市",
    "511600" to "广安市",
    "511700" to "达州市",
    "511800" to "雅安市",
    "511900" to "巴中市",
    "512000" to "资阳市",
    "513200" to "阿坝藏族羌族自治州",
    "513300" to "甘孜藏族自治州",
    "513400" to "凉山彝族自治州",

    "520000" to "贵州省",
    "520100" to "贵阳市",
    "520200" to "六盘水市",
    "520300" to "遵义市",
    "520400" to "安顺市",
    "520500" to "毕节市",
    "520600" to "铜仁市",
    "522300" to "黔西南布依族苗族自治州",
    "522600" to "黔东南苗族侗族自治州",
    "522700" to "黔南布依族苗族自治州",

    "530000" to "云南省",
    "530100" to "昆明市",
    "530300" to "曲靖市",
    "530400" to "玉溪市",
    "530500" to "保山市",
    "530600" to "昭通市",
    "530700" to "丽江市",
    "530800" to "普洱市",
    "530900" to "临沧市",
    "532300" to "楚雄彝族自治州",
    "532500" to "红河哈尼族彝族自治州",
    "532600" to "文山壮族苗族自治州",
    "532800" to "西双版纳傣族自治州",
    "532900" to "大理白族自治州",
    "533100" to "德宏傣族景颇族自治州",
    "533300" to "怒江傈僳族自治州",
    "533400" to "迪庆藏族自治州",

    "540000" to "西藏自治区",
    "540100" to "拉萨市",
    "540200" to "日喀则市",
    "540300" to "昌都市",
    "540400" to "林芝市",
    "540500" to "山南市",
    "540600" to "那曲市",
    "542500" to "阿里地区",

    "610000" to "陕西省",
    "610100" to "西安市",
    "610200" to "铜川市",
    "610300" to "宝鸡市",
    "610400" to "咸阳市",
    "610500" to "渭南市",
    "610600" to "延安市",
    "610700" to "汉中市",
    "610800" to "榆林市",
    "610900" to "安康市",
    "611000" to "商洛市",

    "620000" to "甘肃省",
    "620100" to "兰州市",
    "620200" to "嘉峪关市",
    "620300" to "金昌市",
    "620400" to "白银市",
    "620500" to "天水市",
    "620600" to "武威市",
    "620700" to "张掖市",
    "620800" to "平凉市",
    "620900" to "酒泉市",
    "621000" to "庆阳市",
    "621100" to "定西市",
    "621200" to "陇南市",
    "622900" to "临夏回族自治州",
    "623000" to "甘南藏族自治州",

    "630000" to "青海省",
    "630100" to "西宁市",
    "630200" to "海东市",
    "632200" to "海北藏族自治州",
    "632300" to "黄南藏族自治州",
    "632500" to "海南藏族自治州",
    "632600" to "果洛藏族自治州",
    "632700" to "玉树藏族自治州",
    "632800" to "海西蒙古族藏族自治州",

    "640000" to "宁夏回族自治区",
    "640100" to "银川市",
    "640200" to "石嘴山市",
    "640300" to "吴忠市",
    "640400" to "固原市",
    "640500" to "中卫市",

    "650000" to "新疆维吾尔自治区",
    "650100" to "乌鲁木齐市",
    "650200" to "克拉玛依市",
    "650400" to "吐鲁番市",
    "650500" to "哈密市",
    "652300" to "昌吉回族自治州",
    "652700" to "博尔塔拉蒙古自治州",
    "652800" to "巴音郭楞蒙古自治州",
    "652900" to "阿克苏地区",
    "653000" to "克孜勒苏柯尔克孜自治州",
    "653100" to "喀什地区",
    "653200" to "和田地区",
    "654000" to "伊犁哈萨克自治州",
    "654200" to "塔城地区",
    "654300" to "阿勒泰地区",
    "659001" to "石河子市",
    "659002" to "阿拉尔市",
    "659003" to "图木舒克市",
    "659004" to "五家渠市",
    "659005" to "北屯市",
    "659006" to "铁门关市",
    "659007" to "双河市",
    "659008" to "可克达拉市",
    "659009" to "昆玉市",
    "659010" to "胡杨河市",
    "710000" to "台湾省",
    "810000" to "香港特别行政区",
    "820000" to "澳门特别行政区",
)

fun String?.to2LevelName(): String {
    if (this == null) return ""
    if (!this.isValidRegionCode()) return this

    val secondLevelName = administrativeDivisions[this] ?: return this
    if (this.endsWith("0000")) return secondLevelName   // 例：110000返回“北京市”而不是“北京市-北京市”

    // 绝大部分情况返回XXXX省-XXXX市
    return administrativeDivisions[this.substring(0, 2) + "0000"]  + "-"+ secondLevelName
}

fun String.isValidRegionCode(): Boolean {
    // 正则表达式：^ 表示开始，\d{6} 表示6位数字，$ 表示结束
    val regex = "^\\d{6}$".toRegex()
    return regex.matches(this)
}