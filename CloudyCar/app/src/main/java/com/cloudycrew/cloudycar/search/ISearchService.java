package com.cloudycrew.cloudycar.search;

import com.cloudycrew.cloudycar.models.Point;
import com.cloudycrew.cloudycar.models.requests.PendingRequest;
import com.cloudycrew.cloudycar.models.requests.Request;

import java.util.List;

/**
 * Created by George on 2016-10-13.
 */

public interface ISearchService {
    List<PendingRequest> searchWithPoint(Point point);
    List<PendingRequest> searchWithKeyword(String keyword);
}
