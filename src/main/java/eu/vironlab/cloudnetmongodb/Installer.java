/**
 *   Copyright © 2020 | vironlab.eu | All Rights Reserved.
 *
 *      ___    _______                        ______         ______  
 *      __ |  / /___(_)______________ _______ ___  / ______ ____  /_ 
 *      __ | / / __  / __  ___/_  __ \__  __ \__  /  _  __ `/__  __ \
 *      __ |/ /  _  /  _  /    / /_/ /_  / / /_  /___/ /_/ / _  /_/ /
 *      _____/   /_/   /_/     \____/ /_/ /_/ /_____/\__,_/  /_.___/ 
 *
 *    ____  _______     _______ _     ___  ____  __  __ _____ _   _ _____ 
 *   |  _ \| ____\ \   / / ____| |   / _ \|  _ \|  \/  | ____| \ | |_   _|
 *   | | | |  _|  \ \ / /|  _| | |  | | | | |_) | |\/| |  _| |  \| | | |  
 *   | |_| | |___  \ V / | |___| |__| |_| |  __/| |  | | |___| |\  | | |  
 *   |____/|_____|  \_/  |_____|_____\___/|_|   |_|  |_|_____|_| \_| |_|  
 *
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Contact:
 *
 *     Discordserver:   https://discord.gg/wvcX92VyEH
 *     Website:         https://vironlab.eu/ 
 *     Mail:            contact@vironlab.eu
 *
 */

package eu.vironlab.cloudnetmongodb;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeString;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Installer {

    private CloudNetMongoDB cloudNetMongoDB;
    private ConsoleQuestionListAnimation animation;

    @SneakyThrows
    public Installer(CloudNetMongoDB cloudNetMongoDB) {
        this.cloudNetMongoDB = cloudNetMongoDB;
        IConsole console = CloudNet.getInstance().getConsole();
        IFormatter logFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : new DefaultLogFormatter();
        this.animation = new ConsoleQuestionListAnimation(
                "CloudNet-MongoDB Installer",
                () -> CloudNet.getInstance().getQueuedConsoleLogHandler().getCachedQueuedLogEntries().stream().map(logFormatter::format).collect(Collectors.toList()),
                () -> "&b _______________               ______________   __      _____                       &a______  ___                               ________ ________ \n" +
                        "&b __  ____/___  /______ ____  ________  /___  | / /_____ __  /_                      &a___   |/  /______ _______ _______ _______ ___  __ \\___  __ )\n" +
                        "&b _  /     __  / _  __ \\_  / / /_  __  / __   |/ / _  _ \\_  __/   &7    ________       &a__  /|_/ / _  __ \\__  __ \\__  __ `/_  __ \\__  / / /__  __  |\n" +
                        "&b / /___   _  /  / /_/ // /_/ / / /_/ /  _  /|  /  /  __// /_         &7_/_____/       &a_  /  / /  / /_/ /_  / / /_  /_/ / / /_/ /_  /_/ / _  /_/ / \n" +
                        "&b \\____/   /_/   \\____/ \\__,_/  \\__,_/   /_/ |_/   \\___/ \\__/                    &a    /_/  /_/   \\____/ /_/ /_/ _\\__, /  \\____/ /_____/  /_____/  \n" +
                        "&b                                                                                        &a                      /____/                            ",
                () -> "Successfull installed CloudNet-MongoDB. Please edit the provider in the local/registry file and restart the Cloud",
                "&r» &a"
        );
        this.animation.addEntry(new QuestionListEntry<>(
                "host",
                "Please enter the host of the Database",
                new QuestionAnswerTypeString()
        ));
        this.animation.addEntry(new QuestionListEntry<>(
                "port",
                "Please enter the port of the Database",
                new QuestionAnswerTypeInt()
        ));
        this.animation.addEntry(new QuestionListEntry<>(
                "database",
                "Please enter the database name",
                new QuestionAnswerTypeString()
        ));
        this.animation.addEntry(new QuestionListEntry<>(
                "user",
                "Please enter the Database username",
                new QuestionAnswerTypeString()
        ));
        this.animation.addEntry(new QuestionListEntry<>(
                "password",
                "Please enter the Database password",
                new QuestionAnswerTypeString()
        ));
        this.animation.addEntry(new QuestionListEntry<>(
                "migrate",
                "Did you want to migrate existing data",
                new QuestionAnswerTypeBoolean()
        ));
        this.animation.addFinishHandler(() -> {
            JsonDocument database = new JsonDocument();
            database.getString("host", (String) animation.getResult("host"));
            database.getInt("port", (Integer) animation.getResult("port"));
            database.getString("database", (String) animation.getResult("database"));
            database.getString("user", (String) animation.getResult("user"));
            database.getString("password", (String) animation.getResult("password"));
            cloudNetMongoDB.getConfig().getDocument("connection", database);
            cloudNetMongoDB.saveConfig();

            if ((boolean) animation.getResult("migrate")) {
                String providerName = (String) animation.getResult("oldprovider");
                AbstractDatabaseProvider oldProvider = CloudNet.getInstance().getServicesRegistry().getService(AbstractDatabaseProvider.class, providerName);
                MongoDatabaseProvider mongoDatabaseProvider = new MongoDatabaseProvider(database, Executors.newSingleThreadExecutor());
                try {
                    oldProvider.init();
                    mongoDatabaseProvider.init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String[] tables = ((String) animation.getResult("migrationtables")).split(";");
                for (String table : tables) {
                    CloudNet.getInstance().getLogger().info("[MongoDB] Migrating Table: " + table + "...");
                    if (oldProvider.containsDatabase(table)) {
                        Database db = oldProvider.getDatabase(table);
                        db.keys().forEach(key -> {
                            CloudNet.getInstance().getLogger().info("[MongoDB] Migrating Value of Key: " + key + " of table: " + table);
                            JsonDocument document = db.get(key);
                            mongoDatabaseProvider.getDatabase(table).insert(key, document);
                        });
                    }else {
                        CloudNet.getInstance().getLogger().warning("The Table " + table + " does not exist");
                    }
                }
            }

        });
        console.clearScreen();
        console.startAnimation(this.animation);
    }

    @EventListener
    public void handleResponse(SetupResponseEvent event) {
        if (event.getResponseEntry().getKey().equals("migrate")) {
            if (event.getResponseEntry().getQuestion().equals("Did you want to migrate existing data")) {
                if ((boolean) event.getResponse()) {
                    this.animation.addEntry(new QuestionListEntry<>(
                            "oldprovider",
                            "Wich provider did you have used before?",
                            new QuestionAnswerTypeString() {
                                @Override
                                public @Nullable List<String> getCompletableAnswers() {
                                    List<String> list = new ArrayList<>();
                                    list.add("h2");
                                    list.add("mysql");
                                    return list;
                                }
                            }
                    ));
                    this.animation.addEntry(new QuestionListEntry<>(
                            "migrationtables",
                            "Wich tables have to be migrated? Type name;name2 and so on",
                            new QuestionAnswerTypeString() {
                                @Override
                                public @Nullable List<String> getCompletableAnswers() {
                                    return Arrays.asList(new String[]{"cloudnet_cloud_players;cloudNet_module_configuration;cloudnet_permission_users", "cloudnet_cloud_players;cloudNet_module_configuration"});
                                }
                            }
                    ));
                }
            }
        }
    }


}
