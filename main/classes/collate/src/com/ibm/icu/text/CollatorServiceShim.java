/**
*******************************************************************************
* Copyright (C) 2003-2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.text;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.LocaleKeyFactory;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.coll.CollationLoader;
import com.ibm.icu.impl.coll.CollationTailoring;
import com.ibm.icu.text.Collator.CollatorFactory;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

final class CollatorServiceShim extends Collator.ServiceShim {

    Collator getInstance(ULocale locale) {
    // use service cache, it's faster than instantiation
//          if (service.isDefault()) {
//              return new RuleBasedCollator(locale);
//          }
        try {
            ULocale[] actualLoc = new ULocale[1];
            Collator coll = (Collator)service.get(locale, actualLoc);
            if (coll == null) {
                ///CLOVER:OFF
                //Can't really change coll after it's been initialized
                throw new MissingResourceException("Could not locate Collator data", "", "");
                ///CLOVER:ON
            }
            return (Collator) coll.clone();
        }
        catch (CloneNotSupportedException e) {
        ///CLOVER:OFF
            throw new IllegalStateException(e.getMessage());
        ///CLOVER:ON
        }
    }

    Object registerInstance(Collator collator, ULocale locale) {
        // Set the collator locales while registering so that getInstance()
        // need not guess whether the collator's locales are already set properly
        // (as they are by the data loader).
        collator.setLocale(locale, locale);
        return service.registerObject(collator, locale);
    }

    Object registerFactory(CollatorFactory f) {
        class CFactory extends LocaleKeyFactory {
            CollatorFactory delegate;

            CFactory(CollatorFactory fctry) {
                super(fctry.visible());
                this.delegate = fctry;
            }

            public Object handleCreate(ULocale loc, int kind, ICUService srvc) {
                Object coll = delegate.createCollator(loc);
                return coll;
            }

            public String getDisplayName(String id, ULocale displayLocale) {
                ULocale objectLocale = new ULocale(id);
                return delegate.getDisplayName(objectLocale, displayLocale);
            }

            public Set<String> getSupportedIDs() {
                return delegate.getSupportedLocaleIDs();
            }
        }

        return service.registerFactory(new CFactory(f));
    }

    boolean unregister(Object registryKey) {
        return service.unregisterFactory((Factory)registryKey);
    }

    Locale[] getAvailableLocales() {
        // TODO rewrite this to just wrap getAvailableULocales later
        Locale[] result;
        if (service.isDefault()) {
            result = ICUResourceBundle.getAvailableLocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME,
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        } else {
            result = service.getAvailableLocales();
        }
        return result;
    }

    ULocale[] getAvailableULocales() {
        ULocale[] result;
        if (service.isDefault()) {
            result = ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME,
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        } else {
            result = service.getAvailableULocales();
        }
        return result;
    }

    String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
        String id = objectLocale.getName();
        return service.getDisplayName(id, displayLocale);
    }

    private static class CService extends ICULocaleService {
        CService() {
            super("Collator");

            class CollatorFactory extends ICUResourceBundleFactory {
                CollatorFactory() {
                    super(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
                }

                protected Object handleCreate(ULocale uloc, int kind, ICUService srvc) {
                    return makeInstance(uloc);
                }
            }

            this.registerFactory(new CollatorFactory());
            markDefault();
        }
        ///CLOVER:OFF
        // The following method can not be reached by testing
        protected Object handleDefault(Key key, String[] actualIDReturn) {
            if (actualIDReturn != null) {
                actualIDReturn[0] = "root";
            }
            try {
                return makeInstance(ULocale.ROOT);
            }
            catch (MissingResourceException e) {
                return null;
            }
        }
        ///CLOVER:ON
    }

    // Ported from C++ Collator::makeInstance().
    private static final Collator makeInstance(ULocale desiredLocale) {
        Output<ULocale> validLocale = new Output<ULocale>(ULocale.ROOT);
        CollationTailoring t =
            CollationLoader.loadTailoring(desiredLocale, validLocale);
        return new RuleBasedCollator(t, validLocale.value);
    }

    private static ICULocaleService service = new CService();
}
