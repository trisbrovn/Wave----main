package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

@ModuleInfo(name = "KTBlockFly", description = "Augustus KT Scaffold Port", category = ModuleCategory.WORLD)
public class KTBlockFly extends Module {

    // --- Cài đặt giống hệt bản Augustus KT ---
    private final FloatValue yawSpeed = new FloatValue("YawSpeed", 60.0f, 0.0f, 180.0f);
    private final FloatValue pitchSpeed = new FloatValue("PitchSpeed", 60.0f, 0.0f, 180.0f);
    private final BoolValue rayCast = new BoolValue("RayCast", true);
    private final BoolValue adStrafe = new BoolValue("AdStrafe", true);
    private final BoolValue moonWalk = new BoolValue("MoonWalk", false);
    private final BoolValue moveFix = new BoolValue("MoveFix", true);
    private final ListValue silentMode = new ListValue("SilentMode", new String[]{"Switch", "Spoof", "None"}, "Spoof");
    private final FloatValue timerSpeed = new FloatValue("Timer", 1.0f, 0.1f, 4.0f);
    private final BoolValue sameY = new BoolValue("SameY", false);

    private float[] rots = new float[2];
    private BlockPos targetPos;
    private EnumFacing targetFacing;
    private double lastY;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        lastY = mc.thePlayer.posY;
        rots[0] = mc.thePlayer.rotationYaw;
        rots[1] = mc.thePlayer.rotationPitch;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        mc.timer.timerSpeed = timerSpeed.get();

        if (event.getEventState() == EventState.PRE) {
            // Logic tìm block của KT
            findBlockLogic();

            if (targetPos != null) {
                // Tính toán rotation mượt (Augustus Style)
                float[] targetRots = getTargetRotations();
                rots[0] = updateRotation(rots[0], targetRots[0], yawSpeed.get());
                rots[1] = updateRotation(rots[1], targetRots[1], pitchSpeed.get());

                event.setYaw(rots[0]);
                event.setPitch(rots[1]);
                mc.thePlayer.renderYawOffset = rots[0];
                mc.thePlayer.rotationYawHead = rots[0];
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // Thực hiện đặt block (Place Logic)
        if (targetPos != null && targetFacing != null) {
            int slot = getBlockSlot();
            if (slot == -1) return;

            int oldSlot = mc.thePlayer.inventory.currentItem;
            
            // Xử lý Silent Mode
            if (silentMode.get().equalsIgnoreCase("Switch")) {
                mc.thePlayer.inventory.currentItem = slot;
            }

            // Đặt block
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(slot), targetPos, targetFacing, getHitVec())) {
                mc.thePlayer.swingItem();
            }

            if (silentMode.get().equalsIgnoreCase("Switch")) {
                mc.thePlayer.inventory.currentItem = oldSlot;
            }
        }

        // Logic AD-Strafe của KT
        if (adStrafe.get() && MovementUtils.isMoving()) {
            doADStrafe();
        }
    }

    private void findBlockLogic() {
        double yPos = sameY.get() ? lastY - 1 : mc.thePlayer.posY - 1;
        BlockPos pos = new BlockPos(mc.thePlayer.posX, yPos, mc.thePlayer.posZ);

        if (mc.theWorld.getBlockState(pos).getBlock().getMaterial() == Material.air) {
            for (EnumFacing side : EnumFacing.values()) {
                if (side == EnumFacing.UP) continue;
                BlockPos neighbor = pos.offset(side);
                if (!(mc.theWorld.getBlockState(neighbor).getBlock().getMaterial() == Material.air)) {
                    targetPos = neighbor;
                    targetFacing = side.getOpposite();
                    return;
                }
            }
        }
    }

    private float[] getTargetRotations() {
        // Mô phỏng hàm positionRotation của Augustus
        Vec3 hitVec = getHitVec();
        double diffX = hitVec.xCoord - mc.thePlayer.posX;
        double diffY = hitVec.yCoord - (mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight());
        double diffZ = hitVec.zCoord - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        
        return new float[]{yaw, pitch};
    }

    private Vec3 getHitVec() {
        return new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
    }

    private float updateRotation(float current, float target, float maxStep) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        return current + MathHelper.clamp_float(diff, -maxStep, maxStep);
    }

    private void doADStrafe() {
        // Logic di chuyển zigzag đặc trưng của KT để bypass raycast
        if (mc.thePlayer.ticksExisted % 2 == 0) {
            mc.thePlayer.movementInput.moveStrafe = 0.5f;
        } else {
            mc.thePlayer.movementInput.moveStrafe = -0.5f;
        }
    }

    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) return i;
        }
        return -1;
    }
}