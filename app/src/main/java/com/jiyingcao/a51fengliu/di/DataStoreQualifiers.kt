package com.jiyingcao.a51fengliu.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserSelectionDataStore

/*
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppSettingsDataStore
*/
