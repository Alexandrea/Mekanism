package mekanism.client.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import javax.annotation.Nonnull;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.world.World;

public class ScubaBubbleParticle extends BubbleParticle {

    private ScubaBubbleParticle(World world, double posX, double posY, double posZ, double velX, double velY, double velZ) {
        super(world, posX, posY, posZ, velX, velY, velZ);
        //particleScale = (rand.nextFloat() * 0.2F) + 0.3F;
        maxAge *= 2;
    }

    @Override
    public void tick() {
        super.tick();
        age++;
    }

    @Override
    public void func_225606_a_(IVertexBuilder vertexBuilder, ActiveRenderInfo renderInfo, float partialTicks) {
        if (age > 0) {
            super.func_225606_a_(vertexBuilder, renderInfo, partialTicks);
        }
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {

        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(@Nonnull BasicParticleType type, @Nonnull World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ScubaBubbleParticle particle = new ScubaBubbleParticle(world, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.selectSpriteRandomly(this.spriteSet);
            return particle;
        }
    }
}