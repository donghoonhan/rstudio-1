/*
 * AddinsMRUList.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class AddinsMRUList implements SessionInitHandler,
                                      AddinRegistryUpdatedEvent.Handler
{
   @Inject
   public AddinsMRUList(Provider<WorkbenchListManager> pListManager,
                        Provider<AddinsCommandManager> pAddinManager,
                        Session session,
                        EventBus events,
                        Commands commands)
   {
      pListManager_ = pListManager;
      pAddinManager_ = pAddinManager;
      session_ = session;
      events_ = events;
      commands_ = commands;
      
      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(AddinRegistryUpdatedEvent.TYPE, this);
      
      initCommandHandlers();
   }
   
   private void initCommandHandlers()
   {
      addIndexedHandler(commands_.addinsMru0(),   0);
      addIndexedHandler(commands_.addinsMru1(),   1);
      addIndexedHandler(commands_.addinsMru2(),   2);
      addIndexedHandler(commands_.addinsMru3(),   3);
      addIndexedHandler(commands_.addinsMru4(),   4);
      addIndexedHandler(commands_.addinsMru5(),   5);
      addIndexedHandler(commands_.addinsMru6(),   6);
      addIndexedHandler(commands_.addinsMru7(),   7);
      addIndexedHandler(commands_.addinsMru8(),   8);
      addIndexedHandler(commands_.addinsMru9(),   9);
      addIndexedHandler(commands_.addinsMru10(), 10);
      addIndexedHandler(commands_.addinsMru11(), 11);
      addIndexedHandler(commands_.addinsMru12(), 12);
      addIndexedHandler(commands_.addinsMru13(), 13);
      addIndexedHandler(commands_.addinsMru14(), 14);
   }
   
   private void addIndexedHandler(AppCommand command, int index)
   {
      command.addHandler(new AddinCommandHandler(index));
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      mruList_ = pListManager_.get().getAddinsMruList();
      RAddins addins = session_.getSessionInfo().getAddins();
      update(addins);
   }

   @Override
   public void onAddinRegistryUpdated(AddinRegistryUpdatedEvent event)
   {
      update(event.getData());
   }
   
   private void update(final RAddins addins)
   {
      pAddinManager_.get().loadBindings(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            finishUpdate(addins);
         }
      });
   }
   
   private void finishUpdate(RAddins addins)
   {
      // Collect the addins as key-value pairs (so we can sort)
      List<RAddin> addinsList = new ArrayList<RAddin>();
      
      for (String key : JsUtil.asIterable(addins.keys()))
         addinsList.add(addins.get(key));
      
      Collections.sort(addinsList, new Comparator<RAddin>()
      {
         @Override
         public int compare(RAddin o1, RAddin o2)
         {
            boolean r1 = mruList_.contains(o1.getId());
            boolean r2 = mruList_.contains(o2.getId());
            
            // Recently used commands come first.
            if (r1 != r2)
               return r1 ? 1 : -1;
            
            // Otherwise, compare on IDs.
            return o1.getId().compareTo(o2.getId());
         }
      });
      
      // Save the list (so that the dummy commands can be routed properly)
      addinsList_ = addinsList;
      
      KeyMap addinsKeyMap =
            ShortcutManager.INSTANCE.getKeyMap(KeyMapType.ADDIN);
      
      // Populate up to 15 commands.
      manageCommand(commands_.addinsMru0(),  addinsList, addinsKeyMap,  0);
      manageCommand(commands_.addinsMru1(),  addinsList, addinsKeyMap,  1);
      manageCommand(commands_.addinsMru2(),  addinsList, addinsKeyMap,  2);
      manageCommand(commands_.addinsMru3(),  addinsList, addinsKeyMap,  3);
      manageCommand(commands_.addinsMru4(),  addinsList, addinsKeyMap,  4);
      manageCommand(commands_.addinsMru5(),  addinsList, addinsKeyMap,  5);
      manageCommand(commands_.addinsMru6(),  addinsList, addinsKeyMap,  6);
      manageCommand(commands_.addinsMru7(),  addinsList, addinsKeyMap,  7);
      manageCommand(commands_.addinsMru8(),  addinsList, addinsKeyMap,  8);
      manageCommand(commands_.addinsMru9(),  addinsList, addinsKeyMap,  9);
      manageCommand(commands_.addinsMru10(), addinsList, addinsKeyMap, 10);
      manageCommand(commands_.addinsMru11(), addinsList, addinsKeyMap, 11);
      manageCommand(commands_.addinsMru12(), addinsList, addinsKeyMap, 12);
      manageCommand(commands_.addinsMru13(), addinsList, addinsKeyMap, 13);
      manageCommand(commands_.addinsMru14(), addinsList, addinsKeyMap, 14);
   }
   
   private class AddinCommandHandler implements CommandHandler
   {
      public AddinCommandHandler(int index)
      {
         index_ = index;
      }
      
      @Override
      public void onCommand(AppCommand command)
      {
         if (executor_ == null)
            executor_ = new AddinExecutor();
         
         RAddin addin = addinsList_.get(index_);
         executor_.execute(addin);
      }
      
      private final int index_;
      private AddinExecutor executor_;
   }
   
   private void manageCommand(AppCommand command,
                              List<RAddin> addinsList,
                              KeyMap keyMap,
                              int index)
   {
      if (index >= addinsList.size())
      {
         command.setEnabled(false);
         command.setVisible(false);
         return;
      }
      
      RAddin addin = addinsList.get(index);
      
      command.setEnabled(true);
      command.setVisible(true);
      command.setDesc(addin.getDescription());
      command.setLabel(addin.getName());
      
      List<KeySequence> keys = keyMap.getBindings(addin.getId());
      if (keys != null && !keys.isEmpty())
         command.setShortcut(new KeyboardShortcut(keys.get(0)));
   }
   
   public void add(RAddin addin)
   {
      mruList_.prepend(addin.getId());
   }
   
   // Private Members ----
   private WorkbenchList mruList_;
   private List<RAddin> addinsList_;
   
   // Injected ----
   private final Provider<WorkbenchListManager> pListManager_;
   private final Provider<AddinsCommandManager> pAddinManager_;
   private final Session  session_;
   private final EventBus events_;
   private final Commands commands_;

}