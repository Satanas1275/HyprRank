package com.hyprank.mixin;

import com.hyprank.client.TextureCache;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(FontManager.class)
public class FontManagerMixin {

    @Shadow
    private Map<Identifier, FontSet> fontSets;

    @Shadow
    private TextureManager textureManager;

    @Inject(method = "createFont", at = @At("RETURN"))
    private void hyprrank$injectDynamicFonts(CallbackInfoReturnable<?> cir) {
        com.hyprank.HyprRank.LOGGER.info("[hyprrank-debug] FontManager#createFont hook fired");
        TextureCache.injectDynamicFonts(fontSets, textureManager);
    }
}
