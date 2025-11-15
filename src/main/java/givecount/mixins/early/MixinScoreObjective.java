package givecount.mixins.early;

import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScoreObjective.class)
public class MixinScoreObjective {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCtorTail(Scoreboard sb, String name, IScoreObjectiveCriteria criteria, CallbackInfo ci) {
        if (name == null || criteria == null) {
            new RuntimeException("ScoreObjective init parameter null").printStackTrace();
        }
    }
}
