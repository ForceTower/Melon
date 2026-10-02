package dev.forcetower.unes.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.forcetower.uefs.benchmark"

internal fun UiDevice.waitForHome() {
    requireText("Bom dia, Estudante")
    requireText("Algoritmos de Exemplo")
}

internal fun UiDevice.signInIfNeeded() {
    val ready = Until.hasObject(By.text("Bom dia, Estudante"))
    if (wait(ready, 2_000)) return
    requireText("Já tenho matrícula").click()
    requireText("Entrar")
    check(wait(Until.hasObject(By.clazz("android.widget.EditText")), 10_000))
    val fields = findObjects(By.clazz("android.widget.EditText"))
    check(fields.size == 2) { "Expected the synthetic account login fields" }
    fields[0].text = "scenario"
    fields[1].text = "synthetic-only"
    requireText("Entrar").click()
    requireText("Ver meu semestre").click()
    waitForHome()
}

internal fun UiDevice.scrollHome() {
    val center = displayWidth / 2
    repeat(3) {
        swipe(center, displayHeight * 3 / 4, center, displayHeight / 3, 20)
    }
    repeat(3) {
        swipe(center, displayHeight / 3, center, displayHeight * 3 / 4, 20)
    }
    waitForIdle()
}

private fun UiDevice.requireText(text: String): UiObject2 =
    checkNotNull(wait(Until.findObject(By.text(text)), 90_000)) { "Missing journey state: $text" }
