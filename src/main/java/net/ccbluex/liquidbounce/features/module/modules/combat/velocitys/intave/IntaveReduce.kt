/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.intave

import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion

class IntaveReduce : VelocityMode("IntaveReduce") {

    // OPTIONS
    private val reduceFactor = FloatValue("Factor", 0.6f, 0.1f, 1f)
    private val hurtTimeValue = IntegerValue("HurtTime", 9, 1, 10)
    private val pauseOnExplosion = BoolValue("PauseOnExplosion", true)
    private val pauseTicksValue = IntegerValue("PauseTicks", 20, 1, 50)

    // STATE
    private var intaveTick = 0
    private var intaveDamageTick = 0
    private var lastAttackTime = 0L
    private var hasReceivedVelocity = false
    private var pauseTicks = 0

    val tag: String
        get() = "${(reduceFactor.get() * 100).toInt()}%"

    override fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (pauseTicks > 0) {
            pauseTicks--
            return
        }

        if (!hasReceivedVelocity) return
        intaveTick++

        if (thePlayer.hurtTime == 2) {
            intaveDamageTick++
            if (thePlayer.onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                thePlayer.jump()
                intaveTick = 0
            }
            hasReceivedVelocity = false
        }
    }

    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer ?: return

        if (!hasReceivedVelocity) return

        if (player.hurtTime == hurtTimeValue.get() && System.currentTimeMillis() - lastAttackTime <= 8000) {
            player.motionX *= reduceFactor.get()
            player.motionZ *= reduceFactor.get()
        }

        lastAttackTime = System.currentTimeMillis()
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val thePlayer = mc.thePlayer ?: return

        when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID == thePlayer.entityId) {
                    hasReceivedVelocity = true
                }
            }
            is S27PacketExplosion -> {
                if (pauseOnExplosion.get() &&
                    (thePlayer.motionY + packet.func_149144_d()) > 0.0 &&
                    ((thePlayer.motionX + packet.func_149149_c()) != 0.0 || (thePlayer.motionZ + packet.func_149147_e()) != 0.0)
                ) {
                    hasReceivedVelocity = true
                    pauseTicks = pauseTicksValue.get()
                }
            }
        }
    }

    override fun onEnable() {
        reset()
    }

    override fun onDisable() {
        reset()
    }

    private fun reset() {
        hasReceivedVelocity = false
        intaveTick = 0
        intaveDamageTick = 0
        lastAttackTime = 0L
        pauseTicks = 0
    }
}
