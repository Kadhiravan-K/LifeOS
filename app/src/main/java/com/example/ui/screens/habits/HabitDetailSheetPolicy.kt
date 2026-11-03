package com.example.ui.screens.habits

import com.example.domain.model.HabitItem
import com.example.utils.DateUtils

/**
 * Production policy governing reference-date semantics and check-in actions
 * for the Habit Detail Bottom Sheet.
 *
 * Ensures that the Habit Strength Score and Today Action Banner remain
 * strictly anchored to the actual local current date, completely immune to
 * calendar navigation across past or future dates in [HabitsScreen].
 */
object HabitDetailSheetPolicy {

    /**
     * Resolves the reference date used for Habit Strength calculation.
     * Always returns the actual local current date, explicitly ignoring
     * [selectedCalendarDateIso] so that navigating to past or future calendar
     * days never distorts habit strength evaluation.
     */
    fun resolveReferenceDate(
        selectedCalendarDateIso: String,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return DateUtils.todayString(nowMillis = nowMillis)
    }

    /**
     * Resolves the target date for the detail sheet's check-in toggle action (Mark Done / Undo).
     * Always returns the actual local current date, ensuring check-in operations
     * operate on today rather than the calendar-selected date.
     */
    fun resolveToggleDate(
        selectedCalendarDateIso: String,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        return DateUtils.todayString(nowMillis = nowMillis)
    }

    /**
     * Decision boundary determining whether a given opportunity date is eligible
     * for evaluation against the reference date.
     *
     * Rules:
     * 1. Past dates ([opportunityDateIso] < [referenceDateIso]): Always eligible.
     * 2. Current date ([opportunityDateIso] == [referenceDateIso]): Eligible if and only if completed today.
     *    Incomplete opportunities on the current day remain excluded to prevent premature penalization.
     * 3. Future dates ([opportunityDateIso] > [referenceDateIso]): Strictly ineligible; never counted as missed.
     */
    fun isOpportunityEligible(
        opportunityDateIso: String,
        referenceDateIso: String,
        isCompleted: Boolean
    ): Boolean {
        return when {
            opportunityDateIso < referenceDateIso -> true
            opportunityDateIso == referenceDateIso -> isCompleted
            else -> false // Future dates are never eligible
        }
    }

    /**
     * Returns whether the Today Action banner in the detail sheet should reflect
     * a completed state for the given habit.
     */
    fun isTodayBannerCompleted(habit: HabitItem): Boolean {
        return habit.isCompletedToday
    }
}
