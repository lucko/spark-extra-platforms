/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.allay;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.entity.interfaces.EntityPlayer;

import java.util.UUID;

/**
 * @author IWareQ
 */
public class AllayCommandSender extends AbstractCommandSender<CommandSender> {

    public AllayCommandSender(CommandSender delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate.getCommandSenderName();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate instanceof EntityPlayer player) {
            return player.getUUID();
        }

        return null;
    }

    @Override
    public void sendMessage(Component message) {
        this.delegate.sendText(LegacyComponentSerializer.legacySection().serialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPermission(permission);
    }
}
