package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.general;
import mekanism.api.energy.ICableOutputter;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.lasers.ILaserReceptor;
import mekanism.common.LaserManager;
import mekanism.common.LaserManager.LaserInfo;
import mekanism.common.Mekanism;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.IComputerIntegration;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityLaserAmplifier extends TileEntityContainerBlock implements ILaserReceptor, IRedstoneControl, ICableOutputter, IStrictEnergyStorage, IComputerIntegration, ISecurityTile
{
	public static final double MAX_ENERGY = 5E9;
	public double collectedEnergy = 0;
	public double lastFired = 0;

	public double minThreshold = 0;
	public double maxThreshold = 5E9;
	public int ticks = 0;
	public int time = 0;

	public RedstoneControl controlType = RedstoneControl.DISABLED;

	public boolean on = false;

	public Coord4D digging;
	public double diggingProgress;
	
	public boolean emittingRedstone;
	public int currentRedstoneLevel;
	
	public RedstoneOutput outputMode = RedstoneOutput.OFF;
	
	public TileComponentSecurity securityComponent = new TileComponentSecurity(this);

	public TileEntityLaserAmplifier()
	{
		super("LaserAmplifier");
		inventory = new ItemStack[0];
	}

	@Override
	public void receiveLaserEnergy(double energy, EnumFacing side)
	{
		setEnergy(getEnergy() + energy);
	}

	@Override
	public boolean canLasersDig()
	{
		return false;
	}

	@Override
	public void onUpdate()
	{
		if(worldObj.isRemote)
		{
			if(on)
			{
				RayTraceResult mop = LaserManager.fireLaserClient(this, facing, lastFired, worldObj);
				Coord4D hitCoord = mop == null ? null : new Coord4D(mop, worldObj);

				if(hitCoord == null || !hitCoord.equals(digging))
				{
					digging = hitCoord;
					diggingProgress = 0;
				}

				if(hitCoord != null)
				{
					IBlockState blockHit = hitCoord.getBlockState(worldObj);
					TileEntity tileHit = hitCoord.getTileEntity(worldObj);
					float hardness = blockHit.getBlockHardness(worldObj, hitCoord.getPos());

					if(!(hardness < 0 || (LaserManager.isReceptor(tileHit, mop.sideHit) && !(LaserManager.getReceptor(tileHit, mop.sideHit).canLasersDig()))))
					{
						diggingProgress += lastFired;
						
						if(diggingProgress < hardness*general.laserEnergyNeededPerHardness)
						{
							Mekanism.proxy.addHitEffects(hitCoord, mop);
						}
					}
				}

			}
		}
		else {
			boolean prevRedstone = emittingRedstone;
			
			emittingRedstone = false;
			
			if(ticks < time)
			{
				ticks++;
			}
			else {
				ticks = 0;
			}

			if(toFire() > 0)
			{
				double firing = toFire();

				if(!on || firing != lastFired)
				{
					on = true;
					lastFired = firing;
					Mekanism.packetHandler.sendToAllAround(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<Object>())), Coord4D.get(this).getTargetPoint(50D));
				}

				LaserInfo info = LaserManager.fireLaser(this, facing, firing, worldObj);
				Coord4D hitCoord = info.movingPos == null ? null : new Coord4D(info.movingPos, worldObj);

				if(hitCoord == null || !hitCoord.equals(digging))
				{
					digging = hitCoord;
					diggingProgress = 0;
				}

				if(hitCoord != null)
				{
					IBlockState blockHit = hitCoord.getBlockState(worldObj);
					TileEntity tileHit = hitCoord.getTileEntity(worldObj);
					float hardness = blockHit.getBlockHardness(worldObj, hitCoord.getPos());
					
					if(!(hardness < 0 || (LaserManager.isReceptor(tileHit, info.movingPos.sideHit) && !(LaserManager.getReceptor(tileHit, info.movingPos.sideHit).canLasersDig()))))
					{
						diggingProgress += firing;

						if(diggingProgress >= hardness*general.laserEnergyNeededPerHardness)
						{
							LaserManager.breakBlock(hitCoord, true, worldObj);
							diggingProgress = 0;
						}
					}
				}
				
				emittingRedstone = info.foundEntity;

				setEnergy(getEnergy() - firing);
			}
			else if(on)
			{
				on = false;
				diggingProgress = 0;
				Mekanism.packetHandler.sendToAllAround(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList<Object>())), Coord4D.get(this).getTargetPoint(50D));
			}
			
			if(outputMode != RedstoneOutput.ENTITY_DETECTION)
			{
				emittingRedstone = false;
			}
			
			int newRedstoneLevel = getRedstoneLevel();

			if(newRedstoneLevel != currentRedstoneLevel)
			{
				markDirty();
				currentRedstoneLevel = newRedstoneLevel;
			}
			
			if(emittingRedstone != prevRedstone)
			{
				worldObj.notifyNeighborsOfStateChange(getPos(), getBlockType());
			}
		}
	}

	@Override
	public void setEnergy(double energy)
	{
		collectedEnergy = Math.max(0, Math.min(energy, MAX_ENERGY));
	}

	@Override
	public double getEnergy()
	{
		return collectedEnergy;
	}

	public boolean shouldFire()
	{
		return collectedEnergy >= minThreshold && ticks >= time && MekanismUtils.canFunction(this);
	}

	public double toFire()
	{
		return shouldFire() ? Math.min(collectedEnergy, maxThreshold) : 0;
	}
	
	public int getRedstoneLevel()
	{
		if(outputMode != RedstoneOutput.ENERGY_CONTENTS)
		{
			return 0;
		}
		
		double fractionFull = getEnergy()/getMaxEnergy();
		return MathHelper.floor_float((float)(fractionFull * 14.0F)) + (fractionFull > 0 ? 1 : 0);
	}

	@Override
	public ArrayList<Object> getNetworkedData(ArrayList<Object> data)
	{
		super.getNetworkedData(data);

		data.add(on);
		data.add(minThreshold);
		data.add(maxThreshold);
		data.add(time);
		data.add(collectedEnergy);
		data.add(lastFired);
		data.add(controlType.ordinal());
		data.add(emittingRedstone);
		data.add(outputMode.ordinal());

		return data;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isServer())
		{
			switch(dataStream.readInt())
			{
				case 0:
					minThreshold = Math.min(MAX_ENERGY, MekanismUtils.convertToJoules(dataStream.readDouble()));
					break;
				case 1:
					maxThreshold = Math.min(MAX_ENERGY, MekanismUtils.convertToJoules(dataStream.readDouble()));
					break;
				case 2:
					time = dataStream.readInt();
					break;
				case 3:
					outputMode = RedstoneOutput.values()[outputMode.ordinal() == RedstoneOutput.values().length-1 ? 0 : outputMode.ordinal()+1];
					break;
			}
			
			return;
		}
		
		super.handlePacketData(dataStream);
		
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			on = dataStream.readBoolean();
			minThreshold = dataStream.readDouble();
			maxThreshold = dataStream.readDouble();
			time = dataStream.readInt();
			collectedEnergy = dataStream.readDouble();
			lastFired = dataStream.readDouble();
			controlType = RedstoneControl.values()[dataStream.readInt()];
			emittingRedstone = dataStream.readBoolean();
			outputMode = RedstoneOutput.values()[dataStream.readInt()];
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		on = nbtTags.getBoolean("on");
		minThreshold = nbtTags.getDouble("minThreshold");
		maxThreshold = nbtTags.getDouble("maxThreshold");
		time = nbtTags.getInteger("time");
		collectedEnergy = nbtTags.getDouble("collectedEnergy");
		lastFired = nbtTags.getDouble("lastFired");
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];
		outputMode = RedstoneOutput.values()[nbtTags.getInteger("outputMode")];
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setBoolean("on", on);
		nbtTags.setDouble("minThreshold", minThreshold);
		nbtTags.setDouble("maxThreshold", maxThreshold);
		nbtTags.setInteger("time", time);
		nbtTags.setDouble("collectedEnergy", collectedEnergy);
		nbtTags.setDouble("lastFired", lastFired);
		nbtTags.setInteger("controlType", controlType.ordinal());
		nbtTags.setInteger("outputMode", outputMode.ordinal());
		
		return nbtTags;
	}

	@Override
	public RedstoneControl getControlType()
	{
		return controlType;
	}

	@Override
	public void setControlType(RedstoneControl type)
	{
		controlType = type;
	}

	@Override
	public boolean canPulse()
	{
		return true;
	}

	@Override
	public boolean canOutputTo(EnumFacing side)
	{
		return true;
	}

	@Override
	public double getMaxEnergy()
	{
		return MAX_ENERGY;
	}

	private static final String[] methods = new String[] {"getEnergy", "getMaxEnergy"};

	@Override
	public String[] getMethods()
	{
		return methods;
	}

	@Override
	public Object[] invoke(int method, Object[] arguments) throws Exception
	{
		switch(method)
		{
			case 0:
				return new Object[] {getEnergy()};
			case 1:
				return new Object[] {getMaxEnergy()};
			default:
				throw new NoSuchMethodException();
		}
	}
	
	@Override
	public TileComponentSecurity getSecurity() 
	{
		return securityComponent;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing)
	{
		return capability == Capabilities.ENERGY_STORAGE_CAPABILITY
				|| capability == Capabilities.CABLE_OUTPUTTER_CAPABILITY
				|| capability == Capabilities.LASER_RECEPTOR_CAPABILITY
				|| super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing)
	{
		if(capability == Capabilities.ENERGY_STORAGE_CAPABILITY)
			return (T)this;
		if(capability == Capabilities.CABLE_OUTPUTTER_CAPABILITY)
			return (T)this;
		if(capability == Capabilities.LASER_RECEPTOR_CAPABILITY)
			return (T)this;
		return super.getCapability(capability, facing);
	}
	
	public static enum RedstoneOutput
	{
		OFF("off"),
		ENTITY_DETECTION("entityDetection"),
		ENERGY_CONTENTS("energyContents");
		
		private String unlocalizedName;
		
		private RedstoneOutput(String name)
		{
			unlocalizedName = name;
		}
		
		public String getName()
		{
			return LangUtils.localize("gui." + unlocalizedName);
		}
	}
}
