package pokecube.legends.blocks.normalblocks;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MirageLeaveBlock extends LeavesBlock
{
    public MirageLeaveBlock()
    {
        super(Properties.create(Material.LEAVES, MaterialColor.SNOW).sound(SoundType.NYLIUM).hardnessAndResistance(1, 2));
    }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
       if (rand.nextInt(3) == 0) {
          BlockPos blockpos = pos.down();
          if (worldIn.isAirBlock(blockpos)) {
             double d0 = (double)pos.getX() + rand.nextDouble();
             double d1 = (double)pos.getY() - 0.05D;
             double d2 = (double)pos.getZ() + rand.nextDouble();
             worldIn.addParticle(ParticleTypes.DRIPPING_WATER, d0, d1, d2, 0.0D, 0.0D, 0.0D);
          }
       }
    }
}