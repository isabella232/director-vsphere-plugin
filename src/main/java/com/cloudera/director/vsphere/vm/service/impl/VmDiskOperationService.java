/**
 *
 */
package com.cloudera.director.vsphere.vm.service.impl;

import com.cloudera.director.vsphere.vm.service.intf.IVmDiskOperationService;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VmDiskOperationService implements IVmDiskOperationService {

   private final VirtualMachine vm;

   public VmDiskOperationService(VirtualMachine vm) {
      this.vm = vm;
   }

   @Override
   public void addSwapDisk(String targetDatastoreName, long diskSize, String diskMode) throws Exception {
      VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
      VirtualDeviceConfigSpec vdiskSpec = createAddDiskConfigSpec(vm, targetDatastoreName, diskSize, diskMode);
      VirtualDeviceConfigSpec [] vdiskSpecArray = {vdiskSpec};
      vmConfigSpec.setDeviceChange(vdiskSpecArray);
      Task task = vm.reconfigVM_Task(vmConfigSpec);
      task.waitForMe();
   }

   @Override
   public void addDataDisk(String targetDatastoreName, long diskSize, String diskMode) throws Exception {
      VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
      VirtualDeviceConfigSpec vdiskSpec = createAddDiskConfigSpec(vm, targetDatastoreName, diskSize, diskMode);
      VirtualDeviceConfigSpec [] vdiskSpecArray = {vdiskSpec};
      vmConfigSpec.setDeviceChange(vdiskSpecArray);
      Task task = vm.reconfigVM_Task(vmConfigSpec);
      task.waitForMe();
   }

   private VirtualDeviceConfigSpec createAddDiskConfigSpec(VirtualMachine vm, String targetDatastoreName, long diskSize, String diskMode) throws Exception
   {
      VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();
      VirtualMachineConfigInfo vmConfig = vm.getConfig();
      VirtualDevice[] vds = vmConfig.getHardware().getDevice();

      VirtualDisk disk =  new VirtualDisk();
      VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();

      int key = 0;
      int unitNumber = 0;
      for(int k=0;k<vds.length;k++)
      {
         if(vds[k].getDeviceInfo().getLabel().equalsIgnoreCase("SCSI Controller 0"))
         {
            key = vds[k].getKey();
         }
      }

      unitNumber = vds.length + 1; //***********************seems NOT correct!!!
      String dsName = targetDatastoreName;
      if(dsName==null)
      {
         return null;
      }
      String fileName = "["+ dsName +"] "+ vm.getName() + "/" + vm.getName() + diskSize + ".vmdk";

      diskfileBacking.setFileName(fileName);
      diskfileBacking.setDiskMode(diskMode);

      disk.setControllerKey(key);
      disk.setUnitNumber(unitNumber);
      disk.setBacking(diskfileBacking);
      disk.setCapacityInKB(1024 * 1024 * diskSize);
      disk.setKey(-1);

      diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
      diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);
      diskSpec.setDevice(disk);
      return diskSpec;
   }

   @Override
   public void removeDisk(String diskName) throws Exception {
      VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
      VirtualDeviceConfigSpec vdiskSpec;
      vdiskSpec = createRemoveDiskConfigSpec(diskName);
      VirtualDeviceConfigSpec [] vdiskSpecArray = {vdiskSpec};
      vmConfigSpec.setDeviceChange(vdiskSpecArray);
      Task task = vm.reconfigVM_Task(vmConfigSpec);
      task.waitForMe();
   }

   private VirtualDeviceConfigSpec createRemoveDiskConfigSpec(String diskName) throws Exception {
      VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();
      VirtualDisk disk = (VirtualDisk) findVirtualDevice(vm.getConfig(), diskName);

      if(disk != null) {
         diskSpec.setOperation(VirtualDeviceConfigSpecOperation.remove);
         diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.destroy);
         diskSpec.setDevice(disk);
         return diskSpec;
      } else {
         System.out.println("No device found: " + diskName);
         return null;
      }
   }


   private VirtualDevice findVirtualDevice(VirtualMachineConfigInfo vmConfig, String name) {
      VirtualDevice [] devices = vmConfig.getHardware().getDevice();
      for(int i=0; i<devices.length; i++) {
         if(devices[i].getDeviceInfo().getLabel().equals(name)) {
            return devices[i];
         }
      }
      return null;
   }
}
