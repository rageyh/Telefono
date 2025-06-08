package org.mineacademy.fo.settings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.checkerframework.checker.units.qual.C;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.FileConfig.AccusativeHelper;

/**
 * Un'implementazione semplice di un file di localizzazione di base.
 * Creiamo automaticamente il file localization/messages_LOCALEPREFIX.yml
 * e lo riempiamo con i valori del file localization/messages_LOCALEPREFIX.yml
 * presente all'interno del jar del plugin.
 */
@SuppressWarnings("unused")
public class SimpleLocalization extends YamlStaticConfig {
    /**
     * Un flag che indica che questa classe è stata caricata
     * <p>
     * caricare automaticamente
     */
    private static boolean localizationClassCalled;
    // --------------------------------------------------------------------
    // Caricamento
    // --------------------------------------------------------------------


    // --------------------------------------------------------------------
    // Versione
    // --------------------------------------------------------------------
    /**
     * Il numero di versione della configurazione, trovato nella chiave "Version" del file.
     * <p>
     * Predefinito a 1 se non impostato nel file.
     */
    public static Integer VERSION = 1;






    // --------------------------------------------------------------------
    // Valori condivisi
    // --------------------------------------------------------------------
    // NB: Queste chiavi sono opzionali - non devi scriverle nei tuoi file messages_X.yml
    // ma se lo fai, utilizzeremo i tuoi valori invece di quelli predefiniti!


    /**
     * Crea e carica il file localization/messages_LOCALEPREFIX.yml.
     * <p>
     * Vedi {@link SimpleSettings#LOCALE_PREFIX} per il prefisso della localizzazione.
     * <p>
     * Il file di localizzazione viene estratto dal jar del plugin nella cartella localization/
     * se non esiste, o aggiornato se è obsoleto.
     */
    @Override
    protected final void onLoad() throws Exception {
        final String localePath = "localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml";
        final Object content = FileUtil.getInternalFileContent(localePath);
        Valid.checkNotNull(content, SimplePlugin.getNamed() + " non supporta la localizzazione: messages_" + SimpleSettings.LOCALE_PREFIX
                + ".yml (Per una localizzazione personalizzata, imposta la Localizzazione su 'en' e modifica il file inglese)");
        this.loadConfiguration(localePath);
    }
    /**
     * Imposta e aggiorna automaticamente la versione della configurazione, tuttavia la {@link #VERSION} conterrà
     * la vecchia versione utilizzata nel file sul disco in modo da poterla utilizzare
     * per i confronti nei metodi init()
     * <p>
     * Chiama questo metodo come super metodo quando lo sovrascrivi!
     */
    @Override
    protected final void preLoad() {
        // Carica la versione prima di procedere
        setPathPrefix(null);
        if (isSetDefault("Version"))
            if ((VERSION = getInteger("Version")) != this.getConfigVersion())
                set("Version", this.getConfigVersion());
    }
    /**
     * Restituisce l'ultima versione della configurazione
     * <p>
     * Qualsiasi modifica qui deve essere fatta anche alla chiave "Version" nel file delle impostazioni.
     *
     * @return
     */
    protected int getConfigVersion() {
        return 1;
    }
    /**
     * Mantieni sempre il file lingua aggiornato.
     */
    @Override
    protected final boolean alwaysSaveOnLoad() {
        return true;
    }
    /**
     * Chiavi relative ai comandi del plugin
     */
    public static final class Commands {
        /**
         * Il messaggio alla chiave "No_Console" mostrato quando la console non può eseguire un comando.
         */
        public static String NO_CONSOLE = "&cPuoi utilizzare questo comando solo come giocatore";
        /**
         * Il messaggio mostrato quando la console esegue un comando senza specificare il nome del giocatore
         */
        public static String CONSOLE_MISSING_PLAYER_NAME = "Quando esegui da console, specifica il nome del giocatore.";
        /**
         * Il messaggio mostrato quando si verifica un errore fatale nell'esecuzione del comando
         */
        public static String COOLDOWN_WAIT = "&cAttendi {duration} secondo(i) prima di utilizzare nuovamente questo comando.";
        /**
         * Le chiavi sotto indicano un'azione o un input non valido
         */
        public static String INVALID_ARGUMENT = "&cArgomento non valido. Esegui &6/{label} ? &cper aiuto.";
        public static String INVALID_SUB_ARGUMENT = "&cArgomento non valido. Esegui '/{label} {0}' per aiuto.";
        public static String INVALID_ARGUMENT_MULTILINE = "&cArgomento non valido. Utilizzo:";
        public static String INVALID_TIME = "Il tempo deve essere come '3 ore' o '15 minuti'. Trovato: '{input}'";
        public static String INVALID_NUMBER = "Il numero deve essere un numero intero o decimale. Trovato: '{input}'";
        public static String INVALID_STRING = "Stringa non valida. Trovata: '{input}'";
        public static String INVALID_WORLD = "Mondo '{world}' non valido. Disponibili: {available}";
        /**
         * L'etichetta degli autori
         */
        public static String LABEL_AUTHORS = "Creato da";
        /**
         * L'etichetta della descrizione
         */
        public static String LABEL_DESCRIPTION = "&c&lDescrizione:";
        /**
         * L'etichetta degli argomenti opzionali
         */
        public static String LABEL_OPTIONAL_ARGS = "argomenti opzionali";
        /**
         * L'etichetta degli argomenti richiesti
         */
        public static String LABEL_REQUIRED_ARGS = "argomenti richiesti";
        /**
         * L'etichetta dell'utilizzo
         */
        public static String LABEL_USAGE = "&c&lUtilizzo:";
        /**
         * L'etichetta dell'aiuto
         */
        public static String LABEL_HELP_FOR = "Aiuto per /{label}";
        /**
         * L'etichetta mostrata quando si costruiscono sottocomandi
         */
        public static String LABEL_SUBCOMMAND_DESCRIPTION = " &f/{label} {sublabel} {usage+}{dash+}{description}";
        /**
         * Le chiavi sotto sono utilizzate nel tooltip del menu di aiuto dei comandi.
         */
        public static String HELP_TOOLTIP_DESCRIPTION = "&7Descrizione: &f{description}";
        public static String HELP_TOOLTIP_PERMISSION = "&7Permesso: &f{permission}";
        public static String HELP_TOOLTIP_USAGE = "&7Utilizzo: &f";
        /**
         * Le chiavi sotto sono utilizzate in {@link ReloadCommand}
         */
        public static String RELOAD_DESCRIPTION = "Ricarica la configurazione.";
        public static String RELOAD_STARTED = "Ricaricando i dati del plugin, attendere..";
        public static String RELOAD_SUCCESS = "&6{plugin_name} {plugin_version} è stato ricaricato.";
        public static String RELOAD_FILE_LOAD_ERROR = "&4Oops, &cc'è stato un problema nel caricare i file dal disco! Vedi la console per maggiori informazioni. {plugin_name} non è stato ricaricato.";
        public static String RELOAD_FAIL = "&4Oops, &cricaricamento fallito! Vedi la console per maggiori informazioni. Errore: {error}";
        /**
         * Il messaggio mostrato quando si verifica un errore fatale nell'esecuzione del comando
         */
        public static String ERROR = "&4&lOops! &cIl comando è fallito :( Controlla la console e segnala l'errore.";
        /**
         * Il messaggio mostrato quando non ci sono sottocomandi disponibili
         */
        public static String HEADER_NO_SUBCOMMANDS = "&cNon ci sono argomenti per questo comando.";
        /**
         * Il messaggio mostrato quando il giocatore non ha i permessi per visualizzare i sottocomandi
         */
        public static String HEADER_NO_SUBCOMMANDS_PERMISSION = "&cNon hai i permessi per visualizzare i sottocomandi.";
        /**
         * Il colore principale mostrato nell'intestazione ----- COMANDO -----
         */
        public static ChatColor HEADER_COLOR = ChatColor.GOLD;
        /**
         * Il colore secondario mostrato nell'intestazione ----- COMANDO ----- come in /chc ?
         */
        public static ChatColor HEADER_SECONDARY_COLOR = ChatColor.RED;
        /**
         * Il formato dell'intestazione
         */
        public static String HEADER_FORMAT = "&r";
        /**
         * Il carattere centrale del formato nel caso venga usato \<center\>
         */
        public static String HEADER_CENTER_LETTER = "-;&m<center>&";
        /**
         * Il padding dell'intestazione nel caso venga usato \<center\>
         */
        public static Integer HEADER_CENTER_PADDING = 130;
        /**
         * Chiave per quando il plugin si sta ricaricando {@link org.mineacademy.fo.plugin.SimplePlugin}
         */
        public static String RELOADING = "ricaricando &m&r";
        /**
         * Chiave per quando il plugin è disattivato {@link org.mineacademy.fo.plugin.SimplePlugin}
         */
        public static String DISABLED = "disattivato";
        /**
         * Il messaggio mostrato quando il plugin si sta ricaricando o è stato disattivato e un giocatore tenta di eseguire un comando
         */
        public static String CANNOT_USE_WHILE_NULL = "&cNon puoi usare questo comando mentre il plugin è {state}.";
        /**
         * Il messaggio mostrato in SimpleCommand.findWorld()
         */
        public static String CANNOT_AUTODETECT_WORLD = "Solo i giocatori viventi possono utilizzare ~ per il loro mondo!";
        /**
         * Le chiavi sotto sono utilizzate in {@link DebugCommand}
         */
        public static String DEBUG_DESCRIPTION = "ZIPpa le tue impostazioni per segnalare bug.";
        public static String DEBUG_PREPARING = "&6Preparando il log di debug...";
        public static String DEBUG_SUCCESS = "&2Copiati con successo {amount} file(s) in debug.zip. Le tue informazioni sensibili su MySQL sono state rimosse dai file yml. Caricali tramite ufile.io e inviaceli per revisione.";
        public static String DEBUG_COPY_FAIL = "&cCopia dei file fallita nel file {file} e interrotta. Vedi la console per maggiori informazioni.";
        public static String DEBUG_ZIP_FAIL = "&cCreazione del ZIP fallita, vedi la console per maggiori informazioni. Estrai manualmente la cartella debug/ e inviacela tramite ufile.io.";
        /**
         * Le chiavi sotto sono utilizzate in {@link PermsCommand}
         */
        public static String PERMS_DESCRIPTION = "Elenca tutti i permessi del plugin.";
        public static String PERMS_USAGE = "[frase]";
        public static String PERMS_HEADER = "Elenco di tutti i permessi di {plugin_name}";
        public static String PERMS_MAIN = "Principale";
        public static String PERMS_PERMISSIONS = "Permessi:";
        public static String PERMS_TRUE_BY_DEFAULT = "&7[vero per default]";
        public static String PERMS_INFO = "&7Info: &f";
        public static String PERMS_DEFAULT = "&7Default? ";
        public static String PERMS_APPLIED = "&7Ce l'hai? ";
        public static String PERMS_YES = "&2sì";
        public static String PERMS_NO = "&cno";
        /**
         * Le chiavi sotto sono utilizzate in {@link RegionTool}
         */
        public static String REGION_SET_PRIMARY = "Imposta il punto primario della regione.";
        public static String REGION_SET_SECONDARY = "Imposta il punto secondario della regione.";
        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix("Commands");
            if (isSetDefault("No_Console"))
                NO_CONSOLE = getString("No_Console");
            if (isSetDefault("Console_Missing_Player_Name"))
                CONSOLE_MISSING_PLAYER_NAME = getString("Console_Missing_Player_Name");
            if (isSetDefault("Cooldown_Wait"))
                COOLDOWN_WAIT = getString("Cooldown_Wait");
            if (isSetDefault("Invalid_Argument"))
                INVALID_ARGUMENT = getString("Invalid_Argument");
            if (isSetDefault("Invalid_Sub_Argument"))
                INVALID_SUB_ARGUMENT = getString("Invalid_Sub_Argument");
            if (isSetDefault("Invalid_Argument_Multiline"))
                INVALID_ARGUMENT_MULTILINE = getString("Invalid_Argument_Multiline");
            if (isSetDefault("Invalid_Time"))
                INVALID_TIME = getString("Invalid_Time");
            if (isSetDefault("Invalid_Number"))
                INVALID_NUMBER = getString("Invalid_Number");
            if (isSetDefault("Invalid_String"))
                INVALID_STRING = getString("Invalid_String");
            if (isSetDefault("Invalid_World"))
                INVALID_WORLD = getString("Invalid_World");
            if (isSetDefault("Label_Authors"))
                LABEL_AUTHORS = getString("Label_Authors");
            if (isSetDefault("Label_Description"))
                LABEL_DESCRIPTION = getString("Label_Description");
            if (isSetDefault("Label_Optional_Args"))
                LABEL_OPTIONAL_ARGS = getString("Label_Optional_Args");
            if (isSetDefault("Label_Required_Args"))
                LABEL_REQUIRED_ARGS = getString("Label_Required_Args");
            if (isSetDefault("Label_Usage"))
                LABEL_USAGE = getString("Label_Usage");
            if (isSetDefault("Label_Help_For"))
                LABEL_HELP_FOR = getString("Label_Help_For");
            if (isSetDefault("Label_Subcommand_Description"))
                LABEL_SUBCOMMAND_DESCRIPTION = getString("Label_Subcommand_Description");
            if (isSetDefault("Help_Tooltip_Description"))
                HELP_TOOLTIP_DESCRIPTION = getString("Help_Tooltip_Description");
            if (isSetDefault("Help_Tooltip_Permission"))
                HELP_TOOLTIP_PERMISSION = getString("Help_Tooltip_Permission");
            if (isSetDefault("Help_Tooltip_Usage"))
                HELP_TOOLTIP_USAGE = getString("Help_Tooltip_Usage");
            if (isSetDefault("Reload_Description"))
                RELOAD_DESCRIPTION = getString("Reload_Description");
            if (isSetDefault("Reload_Started"))
                RELOAD_STARTED = getString("Reload_Started");
            if (isSetDefault("Reload_Success"))
                RELOAD_SUCCESS = getString("Reload_Success");
            if (isSetDefault("Reload_File_Load_Error"))
                RELOAD_FILE_LOAD_ERROR = getString("Reload_File_Load_Error");
            if (isSetDefault("Reload_Fail"))
                RELOAD_FAIL = getString("Reload_Fail");
            if (isSetDefault("Error"))
                ERROR = getString("Error");
            if (isSetDefault("Header_No_Subcommands"))
                HEADER_NO_SUBCOMMANDS = getString("Header_No_Subcommands");
            if (isSetDefault("Header_No_Subcommands_Permission"))
                HEADER_NO_SUBCOMMANDS_PERMISSION = getString("Header_No_Subcommands_Permission");
            if (isSetDefault("Header_Color"))
                HEADER_COLOR = get("Header_Color", ChatColor.class);
            if (isSetDefault("Header_Secondary_Color"))
                HEADER_SECONDARY_COLOR = get("Header_Secondary_Color", ChatColor.class);
            if (isSetDefault("Header_Format"))
                HEADER_FORMAT = getString("Header_Format");
            if (isSetDefault("Header_Center_Letter")) {
                HEADER_CENTER_LETTER = getString("Header_Center_Letter");
                Valid.checkBoolean(HEADER_CENTER_LETTER.length() == 1, "Header_Center_Letter deve contenere solo 1 lettera, non " + HEADER_CENTER_LETTER.length() + ":" + HEADER_CENTER_LETTER);
            }
            if (isSetDefault("Header_Center_Padding"))
                HEADER_CENTER_PADDING = getInteger("Header_Center_Padding");
            if (isSet("Reloading"))
                RELOADING = getString("Reloading");
            if (isSet("Disabled"))
                DISABLED = getString("Disabled");
            if (isSet("Use_While_Null"))
                CANNOT_USE_WHILE_NULL = getString("Use_While_Null");
            if (isSet("Cannot_Autodetect_World"))
                CANNOT_AUTODETECT_WORLD = getString("Cannot_Autodetect_World");
            if (isSetDefault("Debug_Description"))
                DEBUG_DESCRIPTION = getString("Debug_Description");
            if (isSetDefault("Debug_Preparing"))
                DEBUG_PREPARING = getString("Debug_Preparing");
            if (isSetDefault("Debug_Success"))
                DEBUG_SUCCESS = getString("Debug_Success");
            if (isSetDefault("Debug_Copy_Fail"))
                DEBUG_COPY_FAIL = getString("Debug_Copy_Fail");
            if (isSetDefault("Debug_Zip_Fail"))
                DEBUG_ZIP_FAIL = getString("Debug_Zip_Fail");
            if (isSetDefault("Perms_Description"))
                PERMS_DESCRIPTION = getString("Perms_Description");
            if (isSetDefault("Perms_Usage"))
                PERMS_USAGE = getString("Perms_Usage");
            if (isSetDefault("Perms_Header"))
                PERMS_HEADER = getString("Perms_Header");
            if (isSetDefault("Perms_Main"))
                PERMS_MAIN = getString("Perms_Main");
            if (isSetDefault("Perms_Permissions"))
                PERMS_PERMISSIONS = getString("Perms_Permissions");
            if (isSetDefault("Perms_True_By_Default"))
                PERMS_TRUE_BY_DEFAULT = getString("Perms_True_By_Default");
            if (isSetDefault("Perms_Info"))
                PERMS_INFO = getString("Perms_Info");
            if (isSetDefault("Perms_Default"))
                PERMS_DEFAULT = getString("Perms_Default");
            if (isSetDefault("Perms_Applied"))
                PERMS_APPLIED = getString("Perms_Applied");
            if (isSetDefault("Perms_Yes"))
                PERMS_YES = getString("Perms_Yes");
            if (isSetDefault("Perms_No"))
                PERMS_NO = getString("Perms_No");
            if (isSetDefault("Region_Set_Primary"))
                REGION_SET_PRIMARY = getString("Region_Set_Primary");
            if (isSetDefault("Region_Set_Secondary"))
                REGION_SET_SECONDARY = getString("Region_Set_Secondary");
        }
    }

    /**
     *enota il messaggio "nessuno"
     */
    public static String NONE = "Nessuno";
    /**
     * I
tringhe relative alle conversazioni con il server in attesa di input da chat
     */
    public static final class Conversation {
        /**
         * La chiave utilizzata quando il giocatore vuole conversare ma non è in conversazione.
         */
        public static String CONVERSATION_NOT_CONVERSING = "&cDevi essere in conversazione con il server!";
        /**
         * Chiamato quando la console tenta di iniziare una conversazione
         */
        public static String CONVERSATION_REQUIRES_PLAYER = "Solo i giocatori possono entrare in questa conversazione.";
        /**
         * Chiamato nel try-catch quando si verifica un errore
         */
        public static String CONVERSATION_ERROR = "&cOops! C'è stato un problema in questa conversazione! Contatta l'amministratore per controllare la console.";
        /**
         * Chiamato in {@link org.mineacademy.fo.conversation.SimplePrompt#show(org.bukkit.entity.Player)}
         */
        public static String CONVERSATION_CANCELLED = "La tua risposta in sospeso è stata annullata.";
        /**
         * Chiamato in {@link org.mineacademy.fo.conversation.SimplePrompt#show(org.bukkit.entity.Player)}
         */
        public static String CONVERSATION_CANCELLED_INACTIVE = "La tua risposta in sospeso è stata annullata perché sei stato inattivo.";

        private static void init() {
            setPathPrefix("Conversation");
            if (isSetDefault("Not_Conversing"))
                CONVERSATION_NOT_CONVERSING = getString("Not_Conversing");
            if (isSetDefault("Requires_Player"))
                CONVERSATION_REQUIRES_PLAYER = getString("Requires_Player");
            if (isSetDefault("Conversation_Error"))
                CONVERSATION_ERROR = getString("Error");
            if (isSetDefault("Conversation_Cancelled"))
                CONVERSATION_CANCELLED = getString("Conversation_Cancelled");
            if (isSetDefault("Conversation_Cancelled_Inactive"))
                CONVERSATION_CANCELLED_INACTIVE = getString("Conversation_Cancelled_Inactive");
        }
    }

    /**
     *l messaggio per il giocatore se manca il permesso.
     */
    public static String NO_PERMISSION = "&cPermesso insufficiente ({permission}).";
    /**
     * I
hiavi relative ai giocatori
     */
    public static final class Player {
        /**
         * Messaggio mostrato quando il giocatore non è online su questo server
         */
        public static String NOT_ONLINE = "&cIl giocatore {player} &cnon è online su questo server.";
        /**
         * Messaggio mostrato quando {@link Bukkit#getOfflinePlayer(String)} restituisce che il giocatore non ha mai giocato
         */
        public static String NOT_PLAYED_BEFORE = "&cIl giocatore {player} &cnon ha mai giocato o non siamo riusciti a trovare i suoi dati.";
        /**
         * Messaggio mostrato quando un giocatore offline restituisce null da un UUID dato.
         */
        public static String INVALID_UUID = "&cImpossibile trovare un giocatore dall'UUID {uuid}.";

        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix("Player");
            if (isSetDefault("Not_Online"))
                NOT_ONLINE = getString("Not_Online");
            if (isSetDefault("Not_Played_Before"))
                NOT_PLAYED_BEFORE = getString("Not_Played_Before");
            if (isSetDefault("Invalid_UUID"))
                INVALID_UUID = getString("Invalid_UUID");
        }
    }

    /**
     *l prefisso del server. Esempio: devi usarlo manualmente se stai inviando messaggi
     * dalla console ai giocatori
     */
    public static String SERVER_PREFIX = "[Server]";
    /**
     * I
hiavi relative a {@link ChatPaginator}
     */
    public static final class Pages {
        public static String NO_PAGE_NUMBER = "&cSpecifica il numero di pagina per questo comando.";
        public static String NO_PAGES = "Non ci sono risultati da elencare.";
        public static String NO_PAGE = "Le pagine non contengono il numero di pagina fornito.";
        public static String INVALID_PAGE = "&cIl tuo input '{input}' non è un numero valido.";
        public static String GO_TO_PAGE = "&7Vai alla pagina {page}";
        public static String GO_TO_FIRST_PAGE = "&7Vai alla prima pagina";
        public static String GO_TO_LAST_PAGE = "&7Vai all'ultima pagina";
        public static String[] TOOLTIP = {
                "&7Puoi anche navigare usando il",
                "&7comando nascosto /#flp <pagina>."
        };

        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix("Pages");
            if (isSetDefault("No_Page_Number"))
                NO_PAGE_NUMBER = getString("No_Page_Number");
            if (isSetDefault("No_Pages"))
                NO_PAGES = getString("No_Pages");
            if (isSetDefault("No_Page"))
                NO_PAGE = getString("No_Page");
            if (isSetDefault("Invalid_Page"))
                INVALID_PAGE = getString("Invalid_Page");
            if (isSetDefault("Go_To_Page"))
                GO_TO_PAGE = getString("Go_To_Page");
            if (isSetDefault("Go_To_First_Page"))
                GO_TO_FIRST_PAGE = getString("Go_To_First_Page");
            if (isSetDefault("Go_To_Last_Page"))
                GO_TO_LAST_PAGE = getString("Go_To_Last_Page");
            if (isSetDefault("Tooltip"))
                TOOLTIP = Common.toArray(getStringList("Tooltip"));
        }
    }

    /**
     *l nome localizzato della console. Esempio: Console
     */
    public static String CONSOLE_NAME = "Console";
    /**
     * I
hiavi relative al sistema GUI
     */
    public static final class Menu {
        /**
         * Messaggio mostrato quando il giocatore è online su questo server
         */
        public static String ITEM_DELETED = "&2{item} è stato eliminato.";
        /**
         * Messaggio mostrato quando il giocatore tenta di aprire il menu, ma ha una conversazione in corso.
         */
        public static String CANNOT_OPEN_DURING_CONVERSATION = "&cDigita 'exit' per uscire dalla conversazione prima di aprire il menu.";
        /**
         * Messaggio mostrato in caso di errore
         */
        public static String ERROR = "&cOops! C'è stato un problema con questo menu! Contatta l'amministratore per controllare la console.";
        /**
         * Chiavi relative alla paginazione del menu
         */
        public static String PAGE_PREVIOUS = "&8<< &fPagina {page}";
        public static String PAGE_NEXT = "Pagina {page} &8>>";
        public static String PAGE_FIRST = "&7Prima Pagina";
        public static String PAGE_LAST = "&7Ultima Pagina";
        /**
         * Chiavi relative ai titoli e tooltip del menu
         */
        public static String TITLE_TOOLS = "Menu Strumenti";
        public static String TOOLTIP_INFO = "&fInformazioni Menu";
        public static String BUTTON_RETURN_TITLE = "&4&lRitorna";
        public static String[] BUTTON_RETURN_LORE = {"", "Torna indietro."};

        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix("Menu");
            if (isSetDefault("Item_Deleted"))
                ITEM_DELETED = getString("Item_Deleted");
            if (isSetDefault("Cannot_Open_During_Conversation"))
                CANNOT_OPEN_DURING_CONVERSATION = getString("Cannot_Open_During_Conversation");
            if (isSetDefault("Error"))
                ERROR = getString("Error");
            if (isSetDefault("Page_Previous"))
                PAGE_PREVIOUS = getString("Page_Previous");
            if (isSetDefault("Page_Next"))
                PAGE_NEXT = getString("Page_Next");
            if (isSetDefault("Page_First"))
                PAGE_FIRST = getString("Page_First");
            if (isSetDefault("Page_Last"))
                PAGE_LAST = getString("Page_Last");
            if (isSetDefault("Title_Tools"))
                TITLE_TOOLS = getString("Title_Tools");
            if (isSetDefault("Tooltip_Info"))
                TOOLTIP_INFO = getString("Tooltip_Info");
            if (isSetDefault("Button_Return_Title"))
                BUTTON_RETURN_TITLE = getString("Button_Return_Title");
            if (isSetDefault("Button_Return_Lore"))
                BUTTON_RETURN_LORE = Common.toArray(getStringList("Button_Return_Lore"));
        }
    }

    /**
     *l messaggio quando una sezione manca dal file dati (quello che termina con .db) (tipicamente usiamo
     * questo file per memorizzare valori serializzati come gli areali dai plugin di minigiochi).
     */
    public static String DATA_MISSING = "&c{name} manca delle informazioni del database! Crea {type} solo in-game! Saltando..";

    /**
     * C
hiavi relative agli strumenti
     */
    public static final class Tool {
        /**
         * Il messaggio mostrato quando uno strumento genera un errore.
         */
        public static String ERROR = "&cOops! C'è stato un problema con questo strumento! Contatta l'amministratore per controllare la console.";

        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix("Tool");
            if (isSetDefault("Error"))
                ERROR = getString("Error");
        }
    }

    /**
     *arica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
     */
    private static void init() {
        setPathPrefix(null);
        Valid.checkBoolean(!localizationClassCalled, "Localizzazione già caricata!");
        if (isSetDefault("No_Permission"))
            NO_PERMISSION = getString("No_Permission");
        if (isSetDefault("Server_Prefix"))
            SERVER_PREFIX = getString("Server_Prefix");
        if (isSetDefault("Console_Name"))
            CONSOLE_NAME = getString("Console_Name");
        if (isSetDefault("Data_Missing"))
            DATA_MISSING = getString("Data_Missing");
        if (isSetDefault("None"))
            NONE = getString("None");
        localizationClassCalled = true;
    }

    /**
     * È
hiavi relative ai casi
     */
    public static class Cases {
        public static AccusativeHelper SECOND = AccusativeHelper.of("secondo", "secondi");
        public static AccusativeHelper MINUTE = AccusativeHelper.of("minuto", "minuti");
        public static AccusativeHelper HOUR = AccusativeHelper.of("ora", "ore");
        public static AccusativeHelper DAY = AccusativeHelper.of("giorno", "giorni");
        public static AccusativeHelper WEEK = AccusativeHelper.of("settimana", "settimane");
        public static AccusativeHelper MONTH = AccusativeHelper.of("mese", "mesi");
        public static AccusativeHelper YEAR = AccusativeHelper.of("anno", "anni");

        private static void init() {
            setPathPrefix("Cases");
            if (isSetDefault("Second"))
                SECOND = getCasus("Second");
            if (isSetDefault("Minute"))
                MINUTE = getCasus("Minute");
            if (isSetDefault("Hour"))
                HOUR = getCasus("Hour");
            if (isSetDefault("Day"))
                DAY = getCasus("Day");
            if (isSetDefault("Week"))
                WEEK = getCasus("Week");
            if (isSetDefault("Month"))
                MONTH = getCasus("Month");
            if (isSetDefault("Year"))
                YEAR = getCasus("Year");
        }
    }

    /**
     * stata caricata questa classe?
     *
     * @return
     */
    public static final Boolean isLocalizationCalled() {
        return localizationClassCalled;
    }

    /**
     * R
hiavi relative all'aggiornamento del plugin
     */
    public static final class Update {
        /**
         * Il messaggio se viene trovata una nuova versione ma non scaricata
         */
        public static String AVAILABLE = "&2Una nuova versione di &3{plugin_name}&2 è disponibile.\n&2Versione attuale: &f{current}&2; Nuova versione: &f{new}\n&2URL: &7https://spigotmc.org/resources/{resource_id}/.";
        /**
         * Il messaggio se viene trovata una nuova versione e scaricata
         */
        public static String DOWNLOADED = "&3{plugin_name}&2 è stato aggiornato da {current} a {new}.\n&2Visita &7https://spigotmc.org/resources/{resource_id} &2per maggiori informazioni.\n&2Riavvia il server per caricare la nuova versione.";

        /**
         * Carica i valori -- questo metodo viene chiamato automaticamente tramite reflection nella classe {@link YamlStaticConfig}!
         */
        private static void init() {
            setPathPrefix(null);
            // Aggiornamento da vecchio percorso
            if (isSet("Update_Available"))
                move("Update_Available", "Update.Available");
            setPathPrefix("Update");
            if (isSetDefault("Available"))
                AVAILABLE = getString("Available");
            if (isSetDefault("Downloaded"))
                DOWNLOADED = getString("Downloaded");
        }
    }

    /**
     *esetta il flag che indica che la classe è stata caricata,
     * utilizzato nel ricaricamento.
     */
    public static void resetLocalizationCall() {
        localizationClassCalled = false;
    }
}