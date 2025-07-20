package com.jiyingcao.a51fengliu.api

/**
 * API接口路径常量定义
 * 统一管理所有后端服务的URL路径，便于维护和保持一致性
 * 
 * API endpoint constants for all backend services.
 * Centralizes URL paths for better maintainability and consistency.
 */
object ApiEndpoints {
    
    /** 用户认证相关接口 */
    object Auth {
        const val LOGIN = "/api/mobile/auth/login.json"
        const val LOGOUT = "/api/mobile/auth/logout.json"
    }
    
    /** 用户个人资料相关接口 */
    object User {
        const val PROFILE = "/api/mobile/authUser/detail.json"
        const val FAVORITES = "/api/mobile/authUser/favoritePage.json"
        const val FAVORITE_STREETS = "/api/mobile/authUser/favoriteStreetPage.json"
    }
    
    /** 信息内容相关接口 */
    object Records {
        const val PAGE = "/api/mobile/info/page.json"
        const val DETAIL = "/api/mobile/info/detail.json"
        const val FAVORITE = "/api/mobile/info/favorite.json"
        const val UNFAVORITE = "/api/mobile/info/unfavorite.json"
        const val UPLOAD = "/api/mobile/info/upload.json"
        const val REPORT = "/api/mobile/info/report.json"
    }
    
    /** 商家相关接口 */
    object Merchant {
        const val PAGE = "/api/mobile/merchant/page.json"
        const val DETAIL = "/api/mobile/merchant/detail.json"
        const val CITIES = "/api/mobile/config/merchantCity.json"
    }
    
    /** 暗巷相关接口 */
    object Street {
        const val PAGE = "/api/mobile/street/page.json"
        const val DETAIL = "/api/mobile/street/detail.json"
        const val FAVORITE = "/api/mobile/street/favorite.json"
        const val UNFAVORITE = "/api/mobile/street/unfavorite.json"
    }
}