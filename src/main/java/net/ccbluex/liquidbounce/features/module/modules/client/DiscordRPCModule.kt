/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.special.DiscordRPC
import net.ccbluex.liquidbounce.features.value.*

@ModuleInfo(name = "DiscordRPC", category = ModuleCategory.CLIENT, defaultOn = true) 
class DiscordRPCModule : Module() {

    val showServerValue = BoolValue("ShowServer", true)
    val showNameValue = BoolValue("ShowName", true)
    val showHealthValue = BoolValue("ShowHealth", true)
    val showOtherValue = BoolValue("ShowOther", true)
    val showModuleValue = BoolValue("ShowModule", true)
    val animated = BoolValue("ShouldAnimate?", true)

    override fun onEnable() {
        // Hàm này sẽ được gọi ngay khi client khởi động
        DiscordRPC.run()
    }

    override fun onDisable() {
        DiscordRPC.stop()
    }
}
