package com.jiyingcao.a51fengliu.api.request

/**
 * 用于发送GET请求获取列表数据，包括首页信息流、城市信息流、搜索结果。
 * 使用时需[toMap]转换后传递给Retrofit接口。
 */
data class RecordsRequest(

    //val perPage: Int = 60,

    /**
     * 城市代码。如：广州市为440100
     */
    val cityCode: String = "",

    /**
     * 关键字搜索
     */
    val keywords: String = "",

    /**
     * 排序方式。如：publish表示最新，daily表示(首页)热门
     */
    val sort: String = "",

    /**
     * 页数，默认为1
     */
    val page: Int = 1
) {
    // 数据验证
    fun validate(): Boolean {
        return cityCode.isNotBlank() &&
                page > 0
    }

    /** 转换为Map，并过滤掉空字段 */
    fun toMap(): Map<String, String> {
        return buildMap {
            if (cityCode.isNotBlank()) put("cityCode", cityCode)
            if (keywords.isNotBlank()) put("keywords", keywords)
            if (sort.isNotBlank()) put("sort", sort)
            put("page", page.toString())
        }
    }

    companion object {
        /** 默认请求 */
        fun default() = RecordsRequest()

        /**
         * 首页信息流
         * @param sort 排序方式。daily表示热门，publish表示最新
         * @param page 页数，默认为1
         */
        fun forHome(
            sort: String = "daily",
            page: Int = 1
        ) = RecordsRequest(
            sort = sort,
            page = page
        )

        /**
         * 城市信息流
         * @param cityCode 城市代码。如：广州市为440100
         * @param sort 排序方式。publish最新、weekly一周热门, monthly本月热门, lastMonth上月热门
         * @param page 页数，默认为1
         */
        fun forCity(
            cityCode: String,
            sort: String = "publish",
            page: Int = 1
        ) = RecordsRequest(
            cityCode = cityCode,
            sort = sort,
            page = page
        )

        /**
         * 搜索结果
         * @param cityCode 城市代码。如：广州市为440100
         * @param keywords 关键字
         * @param page 页数，默认为1
         */
        fun forSearch(
            cityCode: String,
            keywords: String,
            page: Int = 1
        ) = RecordsRequest(
            cityCode = cityCode,
            keywords = keywords,
            page = page
        )
    }
}