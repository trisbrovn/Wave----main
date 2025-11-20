package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.vanilla

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.network.play.server.S12PacketEntityVelocity

class LegitVelocity : VelocityMode("Legit") {
    private val modeValue = ListValue("${valuePrefix}Mode", arrayOf("Motion", "Jump", "Both", "3FMC"), "Jump")
    private val jumpReductionValue = BoolValue("${valuePrefix}ExtraReduction", false)
    private val jumpReductionAmountValue = FloatValue("${valuePrefix}ExtraReductionAmount", 1f, 0.1f, 1f)
    private val motionValue = FloatValue("${valuePrefix}Motion", 0.42f, 0.4f, 0.5f)
    private val failValue = BoolValue("${valuePrefix}SmartFail", true)
    private val failRateValue = FloatValue("${valuePrefix}FailRate", 0.3f, 0.0f, 1.0f).displayable { failValue.get() }
    private val failJumpValue = FloatValue("${valuePrefix}FailJumpRate", 0.25f, 0.0f, 1.0f).displayable { failValue.get() }

    private var doJump = true
    private var failJump = false

    override fun onVelocity(event: UpdateEvent) {
        if ((failJump || mc.thePlayer.hurtTime > 3) && mc.thePlayer.onGround) {
            if (failJump) failJump = false
            if (failValue.get() && Math.random() <= failRateValue.get().toDouble()) {
                if (Math.random() <= failJumpValue.get().toDouble()) {
                    doJump = true
                    failJump = true
                } else {
                    doJump = false
                }
            } else {
                doJump = true
            }
            if (!doJump) return
            when (modeValue.get().lowercase()) {
                "motion" -> mc.thePlayer.motionY = motionValue.get().toDouble()
                "jump" -> mc.thePlayer.jump()
                "both" -> {
                    mc.thePlayer.jump()
                    mc.thePlayer.motionY = motionValue.get().toDouble()
                }
                "3fmc" -> {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                        mc.thePlayer.motionY *= 0.98
                        mc.thePlayer.motionX *= 0.6
                        mc.thePlayer.motionZ *= 0.6
                    }
                }
            }
        }
    }

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && jumpReductionValue.get()) {
            packet.motionX = (packet.motionX * jumpReductionAmountValue.get().toDouble()).toInt()
            packet.motionZ = (packet.motionZ * jumpReductionAmountValue.get().toDouble()).toInt()
            if (modeValue.get().equals("3FMC", true)) {
                packet.motionY = (packet.motionY * 0.85).toInt()
            }
        }
    }
}
