package com.jiyingcao.a51fengliu.api

/**
 * 主要的API服务接口
 * 继承所有领域相关的接口，提供统一的API访问入口
 * 保持向后兼容性，同时支持领域分离的架构
 */
interface ApiService : AuthApiService, UserApiService, RecordApiService, MerchantApiService {
    // 所有接口方法都通过继承的接口提供
    // 这里保持空白以维持向后兼容性
    // 具体的接口实现分布在各个领域接口中：
    // - AuthApiService: 认证相关接口
    // - UserApiService: 用户资料相关接口  
    // - RecordApiService: 信息内容相关接口
    // - MerchantApiService: 商家相关接口
}
