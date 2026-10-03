package net.nav.betteractions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.block.BlockState;
import static net.fabricmc.loader.api.FabricLoader.getInstance;
import static net.minecraft.util.JsonHelper.deserialize;
import static net.minecraft.util.JsonHelper.getInt;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

@Mixin(MinecraftClient.class) class BetterActions { @Shadow int itemUseCooldown;
    private static final int ba$placement = ba$config();
    private static int ba$config() {
        int placementCD = 2,miningCD = 0,accurateMiningCD = 1;
        try {
            var config = getInstance().getConfigDir().resolve("better-actions.txt");
            var defaults = "{\n \"blockPlacementDelay\":2,\n \"miningCooldown\":0,\n \"accurateMiningCooldown\":1\n}";

            try {String s; if (!(s = Files.readString(config)).equals(defaults)) { var j = deserialize(s);
                try {if ((placementCD = getInt(j,"blockPlacementDelay",2)) < 0) placementCD = 0;} catch (Exception e) {}
                try {if ((miningCD = getInt(j,"miningCooldown",0)) < 0) miningCD = 0;} catch (Exception e) {}
                try {if ((accurateMiningCD = getInt(j,"accurateMiningCooldown",1)) < 0) accurateMiningCD = 0;} catch (Exception e) {}
            }
            } catch (NoSuchFileException e) {Files.writeString(config,defaults);}
        } catch (Exception e) {}

        if (miningCD != 0 || accurateMiningCD != 1) getInstance().getObjectShare().put("better-actions:x",new int[]{miningCD,accurateMiningCD});
        return placementCD;
    }
    @Redirect(method = "doItemUse",at = @At(value = "INVOKE",target = "Lnet/minecraft/item/ItemStack;getCount()I",ordinal = 0))
    private int placement(ItemStack s) {if (s.getItem() instanceof BlockItem) itemUseCooldown = ba$placement; return s.getCount();}
}
@Mixin(ClientPlayerInteractionManager.class) class BetterActionsBreaking { @Shadow int blockBreakingCooldown;
    private static final int ba$mining,ba$accurateMining;
    static {int miningCD = 0,accurateMiningCD = 1,v[] = (int[]) getInstance().getObjectShare().remove("better-actions:x");
        if (v != null) {miningCD = v[0]; accurateMiningCD = v[1];} ba$mining = miningCD; ba$accurateMining = accurateMiningCD;
    }
    @ModifyConstant(method = "attackBlock",constant = @Constant(intValue = 5))
    private int mining(int v) {if ((v = blockBreakingCooldown) <= 0) return ba$mining; return v;}

    @ModifyConstant(method = "updateBlockBreakingProgress", constant = @Constant(intValue = 5))
    private static int miningProgress(int v) {return ba$mining;}

    @ModifyArg(method = "breakBlock",at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private BlockState accurateMining(BlockState s) {if (ba$accurateMining > 0) blockBreakingCooldown = ba$accurateMining; return s;}
}