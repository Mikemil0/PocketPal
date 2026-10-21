package com.pocketpal.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pocketpal.app.R
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType

/**
 * RF-A3: los ViewModels emiten [ErrorType] (sin texto); esta función es la que
 * convierte el tipo en un recurso de cadena traducible.
 */
@StringRes
fun ErrorType.messageRes(): Int = when (this) {
    ErrorType.InvalidCredentials -> R.string.error_invalid_credentials
    ErrorType.EmptyPassword -> R.string.error_empty_password
    ErrorType.EmailTaken -> R.string.error_email_taken
    ErrorType.NoNetwork -> R.string.error_no_network
    ErrorType.SessionExpired -> R.string.error_session_expired
    ErrorType.GoogleFailed -> R.string.error_google_failed
    ErrorType.InvalidOtp -> R.string.error_invalid_otp
    ErrorType.ExpiredOtp -> R.string.error_expired_otp
    ErrorType.OtpIncomplete -> R.string.error_otp_incomplete
    ErrorType.InvalidEmail -> R.string.error_invalid_email
    ErrorType.InvalidName -> R.string.error_invalid_name
    ErrorType.NameTooLong -> R.string.error_name_too_long
    ErrorType.PasswordTooShort -> R.string.error_password_too_short
    ErrorType.PasswordNeedsUpperAndDigit -> R.string.error_password_upper_digit
    ErrorType.PasswordMismatch -> R.string.error_password_mismatch
    ErrorType.TermsNotAccepted -> R.string.error_terms_not_accepted
    ErrorType.DuplicateCategoryName -> R.string.error_duplicate_category
    ErrorType.CategoryNameEmpty -> R.string.error_category_name_empty
    ErrorType.CategoryNameTooLong -> R.string.error_category_name_too_long
    ErrorType.InvalidMonthlyLimit -> R.string.error_invalid_monthly_limit
    ErrorType.CurrentPasswordIncorrect -> R.string.error_current_password_incorrect
    ErrorType.NewPasswordSameAsCurrent -> R.string.error_new_password_same
    ErrorType.InvalidPhone -> R.string.error_invalid_phone
    ErrorType.SmsUnavailable -> R.string.error_sms_unavailable
    ErrorType.GoalNameEmpty -> R.string.error_goal_name_empty
    ErrorType.GoalNameTooLong -> R.string.error_goal_name_too_long
    ErrorType.DuplicateGoalName -> R.string.error_duplicate_goal
    ErrorType.InvalidGoalAmount -> R.string.error_invalid_goal_amount
    ErrorType.InitialSavedTooHigh -> R.string.error_initial_saved_too_high
    ErrorType.DeadlineInPast -> R.string.error_deadline_in_past
    ErrorType.GoalAmountBelowSaved -> R.string.error_goal_amount_below_saved
    ErrorType.InvalidContributionAmount -> R.string.error_invalid_contribution_amount
    ErrorType.ContributionDateInFuture -> R.string.error_contribution_date_future
    ErrorType.ContributionNoteTooLong -> R.string.error_contribution_note_too_long
    ErrorType.Unknown -> R.string.error_unknown
}

/** Texto localizado de un error, con sus argumentos (p. ej. el correo del código expirado). */
@Composable
fun AuthState.Error.errorMessage(): String =
    stringResource(type.messageRes(), *args.toTypedArray())
