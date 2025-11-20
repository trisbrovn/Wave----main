package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.item.EntityTNTPrimed
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.pow
import kotlin.math.roundToInt

@ModuleInfo(
    name = "TNTTimer",
    description = "Shows a timer above primed TNT.",
    category = ModuleCategory.RENDER
)
class TNTTimer : Module() {

    // Giữ nguyên các Value (để bạn có thể tùy chỉnh sau)
    private val fontValue = net.ccbluex.liquidbounce.features.value.FontValue("Font", Fonts.font35) 
    private val timerScaleValue = FloatValue("Scale", 2.5F, 0.5F, 5F)
    private val background = BoolValue("Background", true)
    
    private val maxRenderDistance = 50.0

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityTNTPrimed) continue
            if (entity.fuse <= 0) continue

            val distanceSq = player.getDistanceSqToEntity(entity)
            if (distanceSq > maxRenderDistance.pow(2)) continue

            val timeRemaining = entity.fuse / 20F
            renderTNTTimer(entity, timeRemaining)
        }
    }

    private fun renderTNTTimer(tnt: EntityTNTPrimed, timeRemaining: Float) {
        // <--- SỬA TẠI ĐÂY: Dùng font mặc định của Minecraft để tránh lỗi biên dịch --->
        val font = mc.fontRendererObj // Dùng font mặc định
        // <--- KẾT THÚC SỬA --->
        
        val renderManager = mc.renderManager
        val player = mc.thePlayer ?: return

        glPushMatrix()
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val x = tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) * mc.timer.renderPartialTicks - renderManager.renderPosX
        val y = tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) * mc.timer.renderPartialTicks - renderManager.renderPosY + 1.5
        val z = tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) * mc.timer.renderPartialTicks - renderManager.renderPosZ

        glTranslated(x, y, z)
        glRotatef(-renderManager.playerViewY, 0f, 1f, 0f)
        glRotatef(renderManager.playerViewX, 1f, 0f, 0f)

        val dynamicScale = ((player.getDistanceToEntity(tnt) / 4F).coerceAtLeast(1F) / 150F) * timerScaleValue.get()
        glScalef(-dynamicScale, -dynamicScale, dynamicScale)

        val totalTime = 4F 
        val progress = timeRemaining / totalTime
        val timerColor = if (progress > 0.5F) {
            Color.getHSBColor((progress - 0.5F) * 0.4F, 1F, 1F)
        } else {
             Color.getHSBColor(progress * 0.4F, 1F, 1F)
        }.rgb 

        val text = "${String.format("%.1f", timeRemaining)}s"
        
        val width = font.getStringWidth(text) / 2f
        
        // Vẽ nền (Background)
        if (background.get()) {
            // <--- SỬA TẠI ĐÂY: Dùng FONT_HEIGHT của FontRendererObj --->
            RenderUtils.drawRect(-width - 2f, -1f, width + 2f, font.FONT_HEIGHT + 1f, Color(0, 0, 0, 120).rgb)
            // <--- KẾT THÚC SỬA --->
        }

        // Vẽ chữ với màu sắc động và bóng đổ (font.drawString trong Minecraft vẫn dùng được)
        font.drawString(text, -width, 0f, timerColor, true)

        glEnable(GL_DEPTH_TEST)
        glEnable(GL_LIGHTING)
        glDisable(GL_BLEND)
        glPopMatrix()
    }
}