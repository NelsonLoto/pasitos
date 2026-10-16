package com.pasitos.app.data.report.di

import com.pasitos.app.data.report.FakeReportRepository
import com.pasitos.app.data.report.ReportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: FakeReportRepository): ReportRepository
}
