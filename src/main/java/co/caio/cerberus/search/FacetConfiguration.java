package co.caio.cerberus.search;

import org.apache.lucene.facet.FacetsConfig;

import static co.caio.cerberus.search.IndexField.*;

public class FacetConfiguration {
    private static FacetsConfig facetsConfig = null;

    public static FacetsConfig getFacetsConfig() {
        if (facetsConfig == null) {
            facetsConfig = new FacetsConfig();
            facetsConfig.setIndexFieldName(FACET_DIM_DIET, FACET_DIET);
            facetsConfig.setMultiValued(FACET_DIM_DIET, true);

            facetsConfig.setIndexFieldName(FACET_DIM_KEYWORD, FACET_KEYWORD);
            facetsConfig.setMultiValued(FACET_DIM_KEYWORD, true);

        }
        return facetsConfig;
    }
}
