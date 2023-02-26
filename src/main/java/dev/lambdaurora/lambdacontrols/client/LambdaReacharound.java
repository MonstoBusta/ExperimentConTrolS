/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambdaControls.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdacontrols.client;

import dev.lambdaurora.lambdacontrols.LambdaControlsFeature;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the reach-around API of ExperimentConTrolS.
 *
 * @version 1.6.2
 * @since 1.3.2
 */
public class LambdaReacharound {
    private BlockHitResult lastReacharoundResult = null;
    private boolean lastReacharoundVertical = false;
    private boolean onSlab = false;

    public void tick(@NotNull MinecraftClient client) {
        this.lastReacharoundResult = this.tryVerticalReachAround(client);
        if (this.lastReacharoundResult == null) {
            this.lastReacharoundResult = this.tryHorizontalReachAround(client);
            this.lastReacharoundVertical = false;
        } else this.lastReacharoundVertical = true;
    }

    /**
     * Returns the last reach around result.
     *
     * @return The last reach around result.
     */
    public @Nullable BlockHitResult getLastReacharoundResult() {
        return this.lastReacharoundResult;
    }

    /**
     * Returns whether the last reach around is vertical.
     *
     * @return True if the reach around is vertical.
     */
    public boolean isLastReacharoundVertical() {
        return this.lastReacharoundVertical;
    }

    /**
     * Returns whether reacharound is available or not.
     *
     * @return True if reacharound is available, else false.
     */
    public boolean isReacharoundAvailable() {
        return LambdaControlsFeature.HORIZONTAL_REACHAROUND.isAvailable() || LambdaControlsFeature.VERTICAL_REACHAROUND.isAvailable();
    }

    private float getPlayerRange(@NotNull MinecraftClient client) {
        return client.interactionManager != null ? client.interactionManager.getReachDistance() : 0.f;
    }

    /**
     * Returns a nullable block hit result if vertical reach-around is possible.
     *
     * @param client The client instance.
     * @return A block hit result if vertical reach-around is possible, else null.
     */
    public @Nullable BlockHitResult tryVerticalReachAround(@NotNull MinecraftClient client) {
        if (!LambdaControlsFeature.VERTICAL_REACHAROUND.isAvailable())
            return null;
        if (client.player == null || client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.MISS
                || !client.player.isOnGround() || client.player.pitch < 80.0F
                || client.player.isRiding())
            return null;

        Vec3d pos = client.player.getCameraPosVec(1.0F);
        Vec3d rotationVec = client.player.getRotationVec(1.0F);
        float range = getPlayerRange(client);
        Vec3d rayVec = pos.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range).add(0, 0.75, 0);
        BlockHitResult result = client.world.raycast(new RaycastContext(pos, rayVec, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = result.getBlockPos().down();
            BlockState state = client.world.getBlockState(blockPos);

            if (client.player.getBlockPos().getY() - blockPos.getY() > 1 && (client.world.isAir(blockPos) || state.getMaterial().isReplaceable())) {
                return new BlockHitResult(result.getPos(), Direction.DOWN, blockPos, false);
            }
        }

        return null;
    }

    /**
     * Returns a nullable block hit result if horizontal reach-around is possible.
     *
     * @param client The client instance.
     * @return A block hit result if horizontal reach-around is possible.
     */
    public @Nullable BlockHitResult tryHorizontalReachAround(@NotNull MinecraftClient client) {
        if (!LambdaControlsFeature.HORIZONTAL_REACHAROUND.isAvailable())
            return null;

        if (client.player != null && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.MISS && client.player.isOnGround() && client.player.pitch >= 44.5F) {
            if (client.player.isRiding())
                return null;
            Vec3d playerPosi = client.player.getPos(); // temporary pos for the next line
            Vec3d playerPos = new Vec3d(playerPosi.getX(), playerPosi.getY() - 1.0, playerPosi.getZ()); // imitates BlockPos playerPos = client.player.getBlockPos().down();
            if (client.player.getY() - playerPos.getY() - 1.0 >= 0.25) {
                playerPos = new Vec3d(playerPos.getX(), playerPos.getY() + 1.0, playerPos.getZ());
                this.onSlab = true;
            } else {
                this.onSlab = false;
            }

            Vec3d targetPos = new Vec3d(client.crosshairTarget.getPos().getX(), client.crosshairTarget.getPos().getY(), client.crosshairTarget.getPos().getZ()).subtract(playerPos);
            Vec3d vector = new Vec3d(MathHelper.clamp(targetPos.getX(), -1.648833156, 1.648833156), 0, MathHelper.clamp(targetPos.getZ(), -1.648833156, 1.648833156)); // to match CTS 8a's horizontal reach
            Vec3d blockPos = playerPos.add(vector);
            BlockPos blockyPos = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()); // for functions that still need BlockPos

            Direction direction = client.player.getHorizontalFacing();


            BlockState state = client.world.getBlockState(blockyPos);
            if (!state.isAir())
                return null;
            BlockState adjacentBlockState = client.world.getBlockState(blockyPos.offset(direction.getOpposite()));
            if (adjacentBlockState.isAir() || adjacentBlockState.getBlock() instanceof FluidBlock || (vector.getX() == 0 && vector.getZ() == 0)) {
                return null;
            }
            return new BlockHitResult(client.crosshairTarget.getPos(), direction, blockyPos, false);
        }
        return null;
    }

    public @NotNull BlockHitResult withSideForReacharound(@NotNull BlockHitResult result, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
            return result;
        return withSideForReacharound(result, Block.getBlockFromItem(stack.getItem()));
    }

    public @NotNull BlockHitResult withSideForReacharound(@NotNull BlockHitResult result, @NotNull Block block) {
        if (block instanceof SlabBlock) {
            if (this.onSlab) result = result.withSide(Direction.UP);
            else result = result.withSide(Direction.DOWN);
        }
        return result;
    }
}
