package com.kotlinadmin.modules.home.services

interface IHomeService {
    suspend fun getActiveTemplateHtml(): String
}
