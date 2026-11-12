package io.github.raghavsatyadev.openplayer.ui.glancewidget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class OpenPlayerGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OpenPlayerGlanceWidget()
}