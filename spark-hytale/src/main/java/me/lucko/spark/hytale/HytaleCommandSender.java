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

package me.lucko.spark.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.UUID;

public class HytaleCommandSender extends AbstractCommandSender<CommandSender> {

    public HytaleCommandSender(CommandSender delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate.getUsername();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate instanceof Player) {
            return this.delegate.getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component component) {
        this.delegate.sendMessage(toHytaleMessage(component));
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPermission(permission);
    }

    @Override
    protected Object getObjectForComparison() {
        UUID uniqueId = getUniqueId();
        if (uniqueId != null) {
            return uniqueId;
        }
        return getName();
    }

    private static Message toHytaleMessage(Component component) {
        if (!(component instanceof TextComponent text)) {
            throw new UnsupportedOperationException("Unsupported component type: " + component.getClass());
        }

        Message message = Message.raw(text.content());

        TextColor color = text.color();
        if (color != null) {
            message.color(color.asHexString());
        }

        TextDecoration.State bold = text.decoration(TextDecoration.BOLD);
        if (bold != TextDecoration.State.NOT_SET) {
            message.bold(bold == TextDecoration.State.TRUE);
        }

        TextDecoration.State italic = text.decoration(TextDecoration.ITALIC);
        if (italic != TextDecoration.State.NOT_SET) {
            message.italic(italic == TextDecoration.State.TRUE);
        }

        ClickEvent clickEvent = text.clickEvent();
        if (clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL) {
            message.link(clickEvent.value());
        }

        message.insertAll(text.children().stream()
                .map(HytaleCommandSender::toHytaleMessage)
                .toList()
        );
        return message;
    }

}
