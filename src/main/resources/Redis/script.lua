-- Segmented sliding window counter --
local map = redis.call("HGETALL", KEYS[1])
local currSeg = tonumber(ARGV[1])
local segmentSize = tonumber(ARGV[2])
local windowSize = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local sm = 0

-- clientId:route: segmentNo, count
for i = 1, #map, 2 do
	local seg = tonumber(map[i])
	-- if seg outside the segment range, delete it
	if seg < currSeg - (windowSize / segmentSize) then
		redis.call("HDEL", KEYS[1], seg)
	-- else just add to sum
	else
		sm = sm + tonumber(map[i + 1])
	end
end

local allowed = 0

if sm < limit then
	redis.call("HINCRBY", KEYS[1], currSeg, 1)
	allowed = 1
end

-- expire the top-level key after one full window + one segment size
redis.call("PEXPIRE", KEYS[1], windowSize + segmentSize)

return allowed
