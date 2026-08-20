package lk.prime.dailyactivity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryDataRepositoryTest {

    @Test
    fun duplicateSalesCodeIsRejected() = runTest {
        val repo = InMemoryDataRepository()
        val staff = StaffProfile("S001", "Test Staff", "0700000000", "Central")
        assertTrue(repo.registerStaff(staff).isSuccess)
        assertTrue(repo.registerStaff(staff).isFailure)
    }

    @Test
    fun lockedPlanCannotBeChanged() = runTest {
        val repo = InMemoryDataRepository()
        repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 10, planLocked = true))
        val result = repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 11, planLocked = true))
        assertTrue(result.isFailure)
    }

    @Test
    fun doneCountCanChangeAfterPlanLockUntilDayEnds() = runTest {
        val repo = InMemoryDataRepository()
        repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 10, planLocked = true))
        val result = repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 10, prospectingDone = 4, planLocked = true))
        assertTrue(result.isSuccess)
    }

    @Test
    fun endedDayCannotBeChanged() = runTest {
        val repo = InMemoryDataRepository()
        repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 10, prospectingDone = 8, planLocked = true, dayLocked = true))
        val result = repo.saveTodayActivity("S001", DailyActivity(prospectingPlan = 10, prospectingDone = 9, planLocked = true, dayLocked = true))
        assertTrue(result.isFailure)
    }
}
