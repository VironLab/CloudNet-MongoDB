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

import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@Getter
public abstract class MongoDatabase implements Database {

    private static final String COLLECTION_KEY = "==key==";

    protected final MongoDatabaseProvider mongoDatabaseProvider;
    protected final String name;
    protected final ExecutorService executorService;
    private final MongoCollection<Document> collection;

    public MongoDatabase(MongoDatabaseProvider mongoDatabaseProvider, String name, ExecutorService executorService) {
        Preconditions.checkNotNull(mongoDatabaseProvider);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(executorService);
        this.executorService = executorService;
        this.mongoDatabaseProvider = mongoDatabaseProvider;
        this.name = name;
        try {
            mongoDatabaseProvider.getMongoDatabase().createCollection(name);
        } catch (MongoCommandException e) {
        }
        this.collection = mongoDatabaseProvider.getMongoDatabase().getCollection(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void clear() {
        collection.deleteMany(new BasicDBObject());
    }

    @Override
    public ITask<Void> clearAsync() {
        return this.schedule(() -> {
            this.clear();
            return null;
        });
    }

    @Override
    public boolean contains(String key) {
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put(COLLECTION_KEY, key);
        return mongoDatabaseProvider.getMongoDatabase().getCollection(this.name).find(basicDBObject).cursor().hasNext();
    }

    @Override
    public @NotNull ITask<Boolean> containsAsync(String key) {
        AtomicBoolean rs = new AtomicBoolean(false);
        return this.schedule(() -> {
            rs.set(this.contains(key));
            return rs.get();
        });
    }

    @Override
    public boolean delete(String key) {
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.append(COLLECTION_KEY, key);
        mongoDatabaseProvider.getMongoDatabase().getCollection(this.name).deleteOne(basicDBObject);
        return true;
    }

    @Override
    public @NotNull ITask<Boolean> deleteAsync(String key) {
        return this.schedule(() -> delete(key));
    }

    @Override
    public Collection<JsonDocument> documents() {
        Collection<JsonDocument> json = new ArrayList<>();
        FindIterable findIterable = getCollection().find();
        List<Document> documents = new ArrayList<>();
        if (findIterable != null) {
            findIterable.cursor().forEachRemaining(i -> {
                documents.add((Document) i);
            });
        }
        documents.forEach(d -> {
            json.add(DocumentUtils.toJsonDocument(d));
        });
        return json;
    }

    @Override
    public @NotNull ITask<Collection<JsonDocument>> documentsAsync() {
        return this.schedule(() -> documents());
    }

    @Override
    public Map<String, JsonDocument> entries() {
        Map<String, JsonDocument> map = new HashMap<>();
        FindIterable iterable = getCollection().find();
        if (iterable != null) {
            iterable.cursor().forEachRemaining(i -> {
                Document d = (Document) i;
                map.put(d.getString(COLLECTION_KEY), DocumentUtils.toJsonDocument(d));
            });
        }
        return map;
    }

    @Override
    public @NotNull ITask<Map<String, JsonDocument>> entriesAsync() {
        return this.schedule(() -> entries());
    }


    @Override
    public Map<String, JsonDocument> filter(BiPredicate<String, JsonDocument> predicate) {
        Preconditions.checkNotNull(predicate);
        Map<String, JsonDocument> map = new HashMap<>();
        FindIterable iterable = getCollection().find();
        if (iterable != null) {
            iterable.cursor().forEachRemaining(i -> {
                Document d = (Document) i;
                JsonDocument jsonDocument = DocumentUtils.toJsonDocument(d);
                String key = d.getString(COLLECTION_KEY);
                if (predicate.test(key, jsonDocument)) {
                    map.put(key, jsonDocument);
                }
            });
        }
        return map;
    }


    @Override
    public @NotNull ITask<Map<String, JsonDocument>> filterAsync(BiPredicate<String, JsonDocument> predicate) {
        return this.schedule(() -> filter(predicate));
    }


    @Override
    public void close() throws Exception {
        //No Option to Close cause of no caching
    }

    @Override
    public JsonDocument get(String key) {
        Preconditions.checkNotNull(key);
        if (contains(key)) {
            BasicDBObject basicDBObject = new BasicDBObject();
            basicDBObject.append(COLLECTION_KEY, key);
            Document d = getMongoDatabaseProvider().getMongoDatabase().getCollection(getName()).find(basicDBObject).first();
            return DocumentUtils.toJsonDocument(d);
        } else {
            return null;
        }
    }

    @Override
    public @NotNull ITask<JsonDocument> getAsync(String key) {
        Preconditions.checkNotNull(key);
        return this.schedule(() -> get(key));
    }

    @Override
    public long getDocumentsCount() {
        return getCollection().countDocuments();
    }

    @Override
    public @NotNull ITask<Long> getDocumentsCountAsync() {
        return this.schedule(() -> getDocumentsCount());
    }

    @Override
    public boolean insert(String key, JsonDocument document) {
        if (!contains(key)) {
            Document d = DocumentUtils.toBson(document);
            d.append(COLLECTION_KEY, key);
            getCollection().insertOne(d);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ITask<Boolean> insertAsync(String key, JsonDocument document) {
        return this.schedule(() -> insert(key, document));
    }

    @Override
    public Collection<String> keys() {
        Collection<String> rs = new ArrayList<>();
        FindIterable findIterable = getCollection().find();
        if (findIterable != null) {
            findIterable.cursor().forEachRemaining(i -> {
                rs.add(((Document) i).getString(COLLECTION_KEY));
            });
        }
        return rs;
    }

    @Override
    public @NotNull ITask<Collection<String>> keysAsync() {
        return this.schedule(() -> keys());
    }


    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(fieldValue);
        List<JsonDocument> documentList = new ArrayList<>();
        Collection<JsonDocument> documents = documents();
        documents.forEach(d -> {
            if (d.contains(fieldName)) {
                if (d.get(fieldName).toString().substring(1, d.get(fieldName).toString().length() - 1).equals(fieldValue)) {
                    documentList.add(d);
                }
            }
        });
        return documentList;
    }

    @Override
    public @NotNull ITask<List<JsonDocument>> getAsync(String fieldName, Object fieldValue) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(fieldValue);
        return this.schedule(() -> get(fieldName, fieldValue));
    }

    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        Preconditions.checkNotNull(filters);
        List<JsonDocument> documents = new ArrayList<>();
        Document filter = DocumentUtils.toBson(filters);
        FindIterable findIterable = getCollection().find(filter);
        if (findIterable != null) {
            AtomicInteger i = new AtomicInteger(1);
            findIterable.iterator().forEachRemaining(f -> {
                System.out.println(i.get());
                i.getAndIncrement();
                Document d = (Document) f;
                documents.add(DocumentUtils.toJsonDocument(d));
            });
        }
        return documents;
    }

    @Override
    public void iterate(BiConsumer<String, JsonDocument> consumer) {
        Preconditions.checkNotNull(consumer);

        FindIterable iterable = getCollection().find();
        if (iterable != null) {
            iterable.cursor().forEachRemaining(i -> {
                Document d = (Document) i;
                JsonDocument document = DocumentUtils.toJsonDocument(d);
                String key = d.getString(COLLECTION_KEY);
                consumer.accept(key, document);
            });

        }

    }


    @Override
    public @NotNull ITask<Void> iterateAsync(BiConsumer<String, JsonDocument> consumer) {
        return this.schedule(() -> {
            iterate(consumer);
            return null;
        });
    }

    @Override
    public @NotNull ITask<List<JsonDocument>> getAsync(JsonDocument filters) {
        Preconditions.checkNotNull(filters);
        return this.schedule(() -> get(filters));
    }

    @Override
    public boolean update(String key, JsonDocument document) {
        if (contains(key)) {
            Document filter = new Document();
            filter.append(COLLECTION_KEY, key);
            Document d = DocumentUtils.toBson(document);
            d.append(COLLECTION_KEY, key);
            getCollection().replaceOne(filter, d);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ITask<Boolean> updateAsync(String key, JsonDocument document) {
        return this.schedule(() -> update(key, document));
    }


    @NotNull
    private <T> ITask<T> schedule(Callable<T> callable) {
        Preconditions.checkNotNull(callable);
        ITask<T> task = new ListenableTask<>(callable);
        this.executorService.execute(() -> {
            try {
                Thread.sleep(0, 100000);
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return task;
    }
}
