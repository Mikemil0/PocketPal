package com.pocketpal.app.navigation

import com.pocketpal.app.data.category.CategoryKind

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Login = "login"
    const val Register = "register"
    const val OtpSignup = "otp_signup"
    const val ForgotEmail = "forgot_email"
    const val ForgotOtp = "forgot_otp"
    const val ForgotNew = "forgot_new"
    const val ForgotDone = "forgot_done"
    const val PermNotif = "perm_notif"
    const val PermCamera = "perm_camera"
    const val PermMic = "perm_mic"
    const val Google = "google"
    const val NoWifi = "no_wifi"
    const val SessionExpired = "session_expired"
    const val Home = "home"
    const val AccountCreated = "account_created"
    const val Tutorial = "tutorial"
    const val ProfileGraph = "profile_graph"
    const val Profile = "profile"
    const val ProfileEdit = "profile/edit"
    const val ProfileCategories = "profile/categories"
    const val ProfileCategoryNew = "profile/category/new/{kind}"
    const val ProfileCategoryDetail = "profile/category/{id}"

    // Fase 8 (§7.5): exportar, ayuda y acerca de
    const val ProfileExport = "profile/export"
    const val ProfileHelp = "profile/help"
    const val ProfileHelpDetail = "profile/help/{index}"
    const val ProfileAbout = "profile/about"
    const val ProfileAboutLicenses = "profile/about/licenses"

    // Fase 5 (§7.5): notificaciones y preferencias generales
    const val ProfileNotifications = "profile/notifications"
    const val ProfilePreferences = "profile/preferences"
    const val ProfileCurrency = "profile/currency"
    const val ProfileLanguage = "profile/language"
    const val ProfileDateFormat = "profile/date-format"

    // Fase 4 y 7 (§7.5): seguridad y teléfono
    const val ProfileSecurity = "profile/security"
    const val ProfilePassword = "profile/password"
    const val ProfilePhone = "profile/phone"
    const val ProfilePhoneCode = "profile/phone/code"

    // Módulo 3 · Metas
    const val Goals = "goals"
    const val GoalNew = "goals/create"
    const val GoalCreated = "goals/created"
    const val GoalDetail = "goals/detail/{id}"
    const val GoalContribute = "goals/detail/{id}/contribute"
    const val GoalEdit = "goals/detail/{id}/edit"

    // Módulo 4 · Movimientos
    const val Movements = "movements"
    const val MovementSearch = "movements/search"
    const val MovementCategories = "movements/categories"
    const val MovementCalendar = "movements/calendar"
    const val MovementDetail = "movements/detail/{id}"
    const val MovementFullDetail = "movements/detail/{id}/full"

    // Módulo 5 · Presupuestos
    const val Budgets = "budgets"
    const val BudgetNew = "budgets/create"
    const val BudgetCategory = "budgets/category"
    const val BudgetCreated = "budgets/created"
    const val BudgetDetail = "budgets/detail/{id}"
    const val BudgetHistory = "budgets/detail/{id}/history"
    const val BudgetEdit = "budgets/detail/{id}/edit"

    // Módulo 6 · Dai
    const val Dai = "dai"
    const val DaiChat = "dai/chat"
    const val DaiQuickActions = "dai/actions"
    const val DaiAlerts = "dai/alerts"
    const val DaiGoals = "dai/goals"
    const val DaiLearn = "dai/learn"
    const val DaiMissingData = "dai/missing-data"
    const val DaiCamera = "dai/receipt"
    const val DaiReceiptReview = "dai/receipt/review"
    const val DaiVoice = "dai/voice"
    const val DaiSettings = "dai/settings"
    const val DaiClearHistory = "dai/settings/clear-history"

    // Módulo 6 · Flujo 5: registrar un movimiento desde el + del inicio.
    const val NewMovementScan = "movements/new/scan"
    const val NewMovementScanReview = "movements/new/scan/review"
    const val NewMovementVoice = "movements/new/voice"
    const val NewMovementConfirm = "movements/new/confirm"
    const val NewMovementManual = "movements/new/manual"
    const val NewMovementSuccess = "movements/new/success"

    fun categoryNew(kind: CategoryKind) = "profile/category/new/${kind.wire}"
    fun categoryDetail(id: String) = "profile/category/$id"
    fun helpDetail(index: Int) = "profile/help/$index"
    fun goalDetail(id: String) = "goals/detail/$id"
    fun goalContribute(id: String) = "goals/detail/$id/contribute"
    fun goalEdit(id: String) = "goals/detail/$id/edit"
    fun movementDetail(id: String) = "movements/detail/$id"
    fun movementFullDetail(id: String) = "movements/detail/$id/full"
    fun budgetDetail(id: String) = "budgets/detail/$id"
    fun budgetHistory(id: String) = "budgets/detail/$id/history"
    fun budgetEdit(id: String) = "budgets/detail/$id/edit"
}
