package site.vstl.UniqueUUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UniqueUUID extends JavaPlugin {
    private static Map<String, UUID> name2uuid;
    private static Map<UUID, String> uuid2name;
    private File data;

    @Override
    public void onEnable() {
        data = new File(getDataFolder(), "storage.xml");
        if (data.isFile()) {
            try {
                name2uuid = new HashMap<>(255);
                uuid2name = new HashMap<>(255);
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(data);
                doc.getDocumentElement().normalize();
                NodeList nodeList = doc.getElementsByTagName("player");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    UUID uid = UUID.fromString(element.getAttribute("uuid"));
                    String usr = element.getAttribute("name");
                    name2uuid.put(usr, uid);
                    uuid2name.put(uid, usr);
                }
            } catch (ParserConfigurationException | SAXException e) {
                getLogger().log(Level.SEVERE, "Exception in parsing xml database");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Exception in reading xml database");
            }
        } else {
            name2uuid = new HashMap<>(255);
            uuid2name = new HashMap<>(255);
        }

        getServer().getPluginManager().registerEvents(new LoginListener(), this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::save, 0, 10 * 20 * 60);
        getLogger().log(Level.INFO, "Enabled");
    }

    public static class LoginListener implements Listener {
        @EventHandler
        public void listen(PlayerLoginEvent event) {
            UUID sourceUUID = event.getPlayer().getUniqueId();
            String sourceName = event.getPlayer().getName();

            UUID storageUUID = name2uuid.get(sourceName);
            String storageName = uuid2name.get(sourceUUID);
            final String kickMsg = "Another account with the same username has already been registered to this server.\n" +
                    "Please try another username.\n \n" +
                    "该用户已经用\"正版\"或者\"皮肤站\"注册，请采取正确方式登录或使用新的ID。";
            if (sourceName.equals(storageName)) {
                // UUID match and Name Match.
                return; // allowed
            } else if (storageName == null) {
                // New to server
                if (storageUUID != null) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
                } else {
                    // Welcome to our server.
                    name2uuid.put(sourceName, sourceUUID);
                    uuid2name.put(sourceUUID, sourceName);
                }
            } else {
                // This account was renamed.
                if (storageUUID == null) {
                    // Target name not used.
                    name2uuid.put(sourceName, sourceUUID);
                    uuid2name.put(sourceUUID, sourceName);
                    name2uuid.remove(storageName); // name updated.
                } else {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
                }
            }
        }
    }

    @Override
    public void onDisable() { 
        save();
    }

    private void save() {
        try {
            data.getParentFile().mkdirs();
            data.createNewFile();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("players");
            doc.appendChild(root);
            for (Map.Entry<String, UUID> entry : name2uuid.entrySet()) {
                Element player = doc.createElement("player");
                player.setAttribute("name", entry.getKey());
                player.setAttribute("uuid", entry.getValue().toString());
                root.appendChild(player);
            }
            FileOutputStream fos = new FileOutputStream(data);
            fos.write(docToString(doc).getBytes());
            fos.close();
            getLogger().log(Level.INFO, "Saved");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception in saving xml database");
        }
    }

    private String docToString(Document document) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting Document to String", e);
        }
    }
}
