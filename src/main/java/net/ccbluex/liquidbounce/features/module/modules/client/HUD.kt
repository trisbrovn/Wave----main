/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager.height
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorBlueValue
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorGreenValue
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.colorRedValue
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.util.*

@ModuleInfo(name = "HUD", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
object HUD : Module() {
    val shadowValue = ListValue("TextShadowMode", arrayOf("LiquidBounce", "Outline", "Default", "Autumn"), "Default")
    private val clolormode = ListValue("ColorMode", arrayOf("Rainbow", "Light Rainbow", "Static", "Double Color", "Default"), "Light Rainbow")
    val hueInterpolation = BoolValue("hueInterpolation", false)
    val movingcolors = BoolValue("MovingColors", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    private val blurValue = BoolValue("Blur", false)
    private val HealthValue = BoolValue("Health", false)
    private val waterMark = BoolValue("Watermark", true)
    val rainbowStartValue = FloatValue("RainbowStart", 0.55f, 0f, 1f)
    val rainbowStopValue = FloatValue("RainbowStop", 0.85f, 0f, 1f)
    val rainbowSaturationValue = FloatValue("RainbowSaturation", 0.45f, 0f, 1f)
    val rainbowBrightnessValue = FloatValue("RainbowBrightness", 0.85f, 0f, 1f)
    val rainbowSpeedValue = IntegerValue("RainbowSpeed", 1500, 500, 7000)
    val arraylistXAxisAnimSpeedValue = IntegerValue("ArraylistXAxisAnimSpeed", 10, 5, 20)
    val arraylistXAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistXAxisAnimType")
    val arraylistXAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistXAxisHotbarAnimOrder").displayable { !arraylistXAxisAnimTypeValue.equals("NONE") }
    val arraylistYAxisAnimSpeedValue = IntegerValue("ArraylistYAxisAnimSpeed", 10, 5, 20)
    val arraylistYAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistYAxisAnimType")
    val arraylistYAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistYAxisHotbarAnimOrder").displayable { !arraylistYAxisAnimTypeValue.equals("NONE") }
    private val fontEpsilonValue = FloatValue("FontVectorEpsilon", 0.5f, 0f, 1.5f)

    // --- NEW ICON/LOGO SETTINGS ---
    private val iconWatermark = BoolValue("IconWatermark", false)
    private val iconX = FloatValue("IconX", 5f, -1000f, 1000f)
    private val iconY = FloatValue("IconY", 10f, -1000f, 1000f)
    private val iconScale = FloatValue("IconScale", 3.0f, 0.1f, 50.0f)
    private val iconMode = ListValue(
        "IconMode", arrayOf(
            "client", "augustus",
            "red", "blue", "green", "aqua",
            "pink", "white", "black", "yellow",
            "orange", "liquidbounceplusplusplus",
            "lb_banner"
        ),
        "client"
    )
    // -------------------------------

    private var lastFontEpsilon = 0f
    private var animatedIconY = 0f // Dùng để làm mịn vị trí Y của Icon/Logo
    private val mc = net.minecraft.client.Minecraft.getMinecraft()

    /**
     * Renders the HUD.
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner) return
        FDPClient.hud.render(false, event.partialTicks)
        if (waterMark.get()) renderWatermark()
        if (HealthValue.get()) mc.fontRendererObj.drawStringWithShadow(
            MathHelper.ceiling_float_int(mc.thePlayer.health).toString(),
            (width / 2 - 4).toFloat(), (height / 2 - 13).toFloat(), if (mc.thePlayer.health <= 15) Color(255, 0, 0).rgb else Color(0, 255, 0).rgb)
        GlStateManager.resetColor()
    }

    /**
     * Renders the watermark. Prioritizes Icon Watermark over Text Watermark for mutual exclusivity.
     */
    private fun renderWatermark() {
        if (iconWatermark.get()) {
            // --- RENDER ICON/IMAGE WATERMARK (When iconWatermark is ON) ---
            renderIconWatermark()
        } else {
            // --- RENDER TEXT WATERMARK (Existing Logic, when iconWatermark is OFF) ---
            var width = 3
            val colors = getClientColors()
            mc.fontRendererObj.drawStringWithShadow(
                "Wave",
                3.0f,
                3.0f,
                colors?.get(0)?.rgb ?: rainbow().rgb
            )
            width += mc.fontRendererObj.getStringWidth("Wave")
            mc.fontRendererObj.drawStringWithShadow(
                " +++",
                width.toFloat(),
                3.0f,
                colors?.get(1)?.rgb ?: rainbow().rgb
            )
        }
    }

    /**
     * Renders the customizable icon/logo based on IconX, IconY, IconScale, and IconMode.
     */
    private fun renderIconWatermark() {
        val x = iconX.get()
        
        // Lần đầu tiên chạy, đặt giá trị Y ban đầu để tránh nhảy
        if (animatedIconY == 0f) animatedIconY = iconY.get()
        
        // Làm mịn vị trí Y (Animation)
        animatedIconY = interpolateFloat(animatedIconY, iconY.get(), 0.1f)
        val y = animatedIconY

        val logoName = iconMode.get().lowercase()
        // Tạo ResourceLocation từ tên mode
        val logo = ResourceLocation("fdpclient/textures/gui/${logoName}.png")

        mc.textureManager.bindTexture(logo)
        GlStateManager.enableBlend()
        GlStateManager.color(1f, 1f, 1f) // Đảm bảo hình ảnh được vẽ với màu trắng (để giữ nguyên màu texture)

        val scaleFactor = iconScale.get()
        
        // Các mode Among Us cũ
        if (logoName in listOf("red", "blue", "green", "pink", "white", "black", "yellow", "aqua", "orange")) {
            // Logic cho ảnh có tỷ lệ khung hình cố định (ví dụ: Among Us: 512x768)
            val originalWidth = 512f
            val originalHeight = 768f
            
            // Tính toán kích thước theo chiều ngang và áp dụng tỷ lệ
            val desiredWidth = (16 * scaleFactor).toInt().coerceAtLeast(1)
            val aspectRatio = originalWidth / originalHeight
            val desiredHeight = (desiredWidth / aspectRatio).toInt()
            
            RenderUtils.drawImage(logo, x.toInt(), y.toInt(), desiredWidth, desiredHeight)
        } else {
            // Logic mặc định cho ảnh vuông (bao gồm liquidbounce, minusbounce, client, lb_banner)
            val logoSize = (16 * scaleFactor).toInt().coerceAtLeast(1)
            RenderUtils.drawImage(logo, x.toInt(), y.toInt(), logoSize, logoSize)
        }
        
        GlStateManager.disableBlend()
        GlStateManager.resetColor() // Đảm bảo đặt lại trạng thái màu sắc sau khi vẽ
    }

    /**
     * Helper function to interpolate a float value for smooth animation.
     */
    private fun interpolateFloat(current: Float, target: Float, speed: Float): Float {
        return current + (target - current) * speed
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        FDPClient.hud.update()
        if (mc.currentScreen == null && lastFontEpsilon != fontEpsilonValue.get()) {
            lastFontEpsilon = fontEpsilonValue.get()
            alert("You need to reload FDPClient to apply changes!")
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        lastFontEpsilon = fontEpsilonValue.get()
    }

    @EventTarget
    fun onScreen(event: ScreenEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return
        }

        if (state && blurValue.get() && !mc.entityRenderer.isShaderActive && event.guiScreen != null && !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)) {
            mc.entityRenderer.loadShader(ResourceLocation("fdpclient/blur.json"))
        } else if (mc.entityRenderer.shaderGroup != null && mc.entityRenderer.shaderGroup!!.shaderGroupName.contains("fdpclient/blur.json")) {
            mc.entityRenderer.stopUseShader()
        }
    }

    @EventTarget
    fun onKey(event: KeyEvent) {
        FDPClient.hud.handleKey('a', event.key)
    }

    fun getClientColors(): Array<Color>? {
        val firstColor: Color
        val secondColor: Color
        when (clolormode.get().lowercase(Locale.getDefault())) {
            "light rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, .6f, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, .6f, 1F, 1F)!!
            }
            "rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, 1F, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, 1F, 1F, 1F)!!
            }
            "double color" -> {
                firstColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 0, Color.PINK, Color.BLUE, hueInterpolation.get())!!
                secondColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 90, Color.PINK, Color.BLUE, hueInterpolation.get())!!
            }
            "static" -> {
                firstColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
                secondColor = firstColor
            }
            else -> {
                firstColor = Color(-1)
                secondColor = Color(-1)
            }
        }
        return arrayOf(firstColor, secondColor)
    }
}
