package com.ninosano.app.data.report

import com.ninosano.app.data.report.model.ReportSection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class FakeReportRepository @Inject constructor() : ReportRepository {

    override suspend fun generateReport(childId: String, sections: Set<ReportSection>): Result<Unit> {
        delay(1200)
        return Result.success(Unit)
    }
}
