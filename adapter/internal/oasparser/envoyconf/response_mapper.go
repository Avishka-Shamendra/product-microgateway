/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package envoyconf

import (
	"strconv"

	access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	envoy_type_matcher_v3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/wso2/product-microgateway/adapter/internal/err"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func getErrorResponseMappers() []*hcmv3.ResponseMapper {
	return []*hcmv3.ResponseMapper{
		genErrorResponseMapper(err.NotFoundCode, err.NotFoundCode, err.NotFoundMessage, err.NotFoundDescription, "NR"),
		genErrorResponseMapper(503, err.UfCode, err.UfMessage, "%LOCAL_REPLY_BODY%", "UF"),
		genErrorResponseMapper(504, err.UtCode, err.UtMessage, "%LOCAL_REPLY_BODY%", "UT"),
		genErrorResponseMapper(503, err.UoCode, err.UoMessage, "%LOCAL_REPLY_BODY%", "UO"),
		genErrorResponseMapper(500, err.UrxCode, err.UrxMessage, "%LOCAL_REPLY_BODY%", "URX"),
		genErrorResponseMapper(500, err.NcCode, err.NcMessage, "%LOCAL_REPLY_BODY%", "NC"),
		genErrorResponseMapper(503, err.UhCode, err.UhMessage, "%LOCAL_REPLY_BODY%", "UH"),
		genErrorResponseMapper(503, err.UrCode, err.UrMessage, "%LOCAL_REPLY_BODY%", "UR"),
		genErrorResponseMapper(503, err.UcCode, err.UcMessage, "%LOCAL_REPLY_BODY%", "UC"),
		genErrorResponseMapper(503, err.LrCode, err.LrMessage, "%LOCAL_REPLY_BODY%", "LR"),
		genErrorResponseMapper(400, err.IhCode, err.IhMessage, "%LOCAL_REPLY_BODY%", "IH"),
		genErrorResponseMapper(500, err.SiCode, err.SiMessage, "%LOCAL_REPLY_BODY%", "SI"),
		genErrorResponseMapper(500, err.DpeCode, err.DpeMessage, "%LOCAL_REPLY_BODY%", "DPE"),
		genErrorResponseMapper(500, err.UpeCode, err.UpeMessage, "%LOCAL_REPLY_BODY%", "UPE"),
		genErrorResponseMapper(500, err.UmsdrCode, err.UmsdrMessage, "%LOCAL_REPLY_BODY%", "UMSDR"),
		genErrorResponseMapper(429, err.RlCode, err.RlMessage, err.RlDescription, "RL"),
		genExtAuthResponseMapper(genExtAuthFilters(), uint32(500), int32(err.UaexCode), err.UaexMessage, err.UaexDecription),
	}
}

func genErrorResponseMapper(statusCode uint32, errorCode int32, message string, description string, flag string) *hcmv3.ResponseMapper {
	errorMsgMap := make(map[string]*structpb.Value)
	errorMsgMap["code"] = structpb.NewStringValue(strconv.FormatInt(int64(errorCode), 10))
	errorMsgMap["message"] = structpb.NewStringValue(message)
	errorMsgMap["description"] = structpb.NewStringValue(description)

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_ResponseFlagFilter{
				ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
					Flags: []string{flag},
				},
			},
		},
		StatusCode: wrapperspb.UInt32(statusCode),
		BodyFormatOverride: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_JsonFormat{
				JsonFormat: &structpb.Struct{
					Fields: errorMsgMap,
				},
			},
		},
	}
	return mapper
}

// genMetadataFilter returns a metadata filter specifer which can be used to check the metadata availability in the ext_authz filter
func genMetadataFilterForExtAuthz() *access_logv3.AccessLogFilter_MetadataFilter {

	return &access_logv3.AccessLogFilter_MetadataFilter{
		MetadataFilter: &access_logv3.MetadataFilter{
			Matcher: &envoy_type_matcher_v3.MetadataMatcher{
				Filter: extAuthzFilterName,
				Value: &envoy_type_matcher_v3.ValueMatcher{
					MatchPattern: &envoy_type_matcher_v3.ValueMatcher_StringMatch{
						StringMatch: &envoy_type_matcher_v3.StringMatcher{
							MatchPattern: &envoy_type_matcher_v3.StringMatcher_Exact{Exact: "Not Matching"},
						},
					},
				},
				Path: []*envoy_type_matcher_v3.MetadataMatcher_PathSegment{{Segment: &envoy_type_matcher_v3.MetadataMatcher_PathSegment_Key{Key: choreoConnectEnforcerReply}}},
			},
			MatchIfKeyNotFound: &wrapperspb.BoolValue{
				Value: true,
			},
		},
	}
}

func genExtAuthResponseMapper(filters []*access_logv3.AccessLogFilter,
	statusCode uint32, errorCode int32, message string, description string) *hcmv3.ResponseMapper {

	errorMsgMap := make(map[string]*structpb.Value)
	errorMsgMap["code"] = structpb.NewStringValue(strconv.FormatInt(int64(errorCode), 10))
	errorMsgMap["message"] = structpb.NewStringValue(message)
	errorMsgMap["description"] = structpb.NewStringValue(description)

	mapper := &hcmv3.ResponseMapper{
		Filter: &access_logv3.AccessLogFilter{
			FilterSpecifier: &access_logv3.AccessLogFilter_AndFilter{
				AndFilter: &access_logv3.AndFilter{
					Filters: filters,
				},
			},
		},
		StatusCode: wrapperspb.UInt32(statusCode),
		BodyFormatOverride: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_JsonFormat{
				JsonFormat: &structpb.Struct{
					Fields: errorMsgMap,
				},
			},
		},
	}
	return mapper
}

func genExtAuthFilters() []*access_logv3.AccessLogFilter {

	filters := []*access_logv3.AccessLogFilter{
		{
			FilterSpecifier: genMetadataFilterForExtAuthz(),
		},
		{
			FilterSpecifier: &access_logv3.AccessLogFilter_ResponseFlagFilter{
				ResponseFlagFilter: &access_logv3.ResponseFlagFilter{
					Flags: []string{uaexCode},
				},
			},
		},
	}

	return filters
}
