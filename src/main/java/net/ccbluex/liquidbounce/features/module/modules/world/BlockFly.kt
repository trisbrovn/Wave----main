package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.material.Material
import net.minecraft.item.ItemBlock
import net.minecraft.util.*

@ModuleInfo(name = "KTBlockFly", description = "Augustus KT Port", category = ModuleCategory.WORLD)
class KTBlockFly : Module() {

    private val yawSpeed = FloatValue("YawSpeed", 60.0f, 0.0f, 180.0f)
    private val pitchSpeed = FloatValue("PitchSpeed", 60.0f, 0.0f, 180.0f)
    private val adStrafe = BoolValue("AdStrafe", true)
    private val silentMode = ListValue("SilentMode", arrayOf("Switch", "Spoof", "None"), "Spoof")
    private val timerSpeed = FloatValue("Timer", 1.0f, 0.1f, 4.0f)

    private var rots = floatArrayOf(0f, 0f)
    private var targetPos: BlockPos? = null
    private var targetFacing: EnumFacing? = null

    override fun onEnable() {
        mc.thePlayer?.let {
            rots[0] = it.rotationYaw
            rots[1] = it.rotationPitch
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        mc.timer.timerSpeed = timerSpeed.get()

        if (event.eventState == EventState.PRE) {
            findBlockLogic()

            targetPos?.let { pos ->
                val targetRots = getTargetRotations(pos)
                rots[0] = updateRotation(rots[0], targetRots[0], yawSpeed.get())
                rots[1] = updateRotation(rots[1], targetRots[1], pitchSpeed.get())

                event.yaw = rots[0]
                event.pitch = rots[1]
                mc.thePlayer.renderYawOffset = rots[0]
                mc.thePlayer.rotationYawHead = rots[0]
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val pos = targetPos
        val facing = targetFacing ?: return
        if (pos == null) return

        val slot = getBlockSlot()
        if (slot == -1) return

        val oldSlot = mc.thePlayer.inventory.currentItem

        if (silentMode.get().equals("Switch", true)) {
            mc.thePlayer.inventory.currentItem = slot
        }

        val hitVec = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(slot), pos, facing, hitVec)) {
            mc.thePlayer.swingItem()
        }

        if (silentMode.get().equals("Switch", true)) {
            mc.thePlayer.inventory.currentItem = oldSlot
        }

        if (adStrafe.get() && MovementUtils.isMoving()) {
            mc.thePlayer.movementInput.moveStrafe = if (mc.thePlayer.ticksExisted % 2 == 0) 0.5f else -0.5f
        }
    }

    private fun findBlockLogic() {
        val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
        if (mc.theWorld.getBlockState(pos).block.material == Material.air) {
            for (side in EnumFacing.values()) {
                if (side == EnumFacing.UP) continue
                val neighbor = pos.offset(side)
                if (mc.theWorld.getBlockState(neighbor).block.material != Material.air) {
                    targetPos = neighbor
                    targetFacing = side.opposite
                    return
                }
            }
        }
    }

    private fun getTargetRotations(pos: BlockPos): FloatArray {
        val diffX = pos.x + 0.5 - mc.thePlayer.posX
        val diffY = pos.y + 0.5 - (mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight)
        val diffZ = pos.z + 0.5 - mc.thePlayer.posZ
        val dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
        val yaw = (Math.atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = (-(Math.atan2(diffY, dist) * 180.0 / Math.PI)).toFloat()
        return floatArrayOf(yaw, pitch)
    }

    private fun updateRotation(current: float, target: float, maxStep: float): Float {
        var diff = MathHelper.wrapAngleTo180_float(target - current)
        if (diff > maxStep) diff = maxStep
        if (diff < -maxStep) diff = -maxStep
        return current + diff
    }

    private fun getBlockSlot(): Int {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is ItemBlock) return i
        }
        return -1
    }
}