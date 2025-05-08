function add_pod_name(tag, timestamp, record)
    if record and record['kubernetes'] then
        record['pod_name'] = record['kubernetes']['pod_name']
    end

    if record and record['@timestamp'] then
      record['when'] = record['@timestamp']
      record['@timestamp'] = nil
    end
    return 1, timestamp, record
end

