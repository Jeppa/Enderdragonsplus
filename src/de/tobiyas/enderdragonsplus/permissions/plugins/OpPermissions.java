package de.tobiyas.enderdragonsplus.permissions.plugins;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpPermissions implements PermissionPlugin {

	private boolean isActive;
	
	public OpPermissions(){
		isActive = true;
	}
	
	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public boolean getPermissions(CommandSender sender, String permissionNode) {
		return sender.isOp();
	}

	@Override
	public ArrayList<String> getGroups() {
		ArrayList<String> emptyList = new ArrayList<String>();
		return emptyList;
	}

	@Override
	public String getGroupOfPlayer(Player player) {
		return "";
	}

	@Override
	public void init() {
		//No inition needed.
	}

	@Override
	public String getName() {
		return "OpPermissions";
	}

}
