#!/usr/bin/env groovy

/*
 * compares source kernel config with target kernel config
 */

import java.util.logging.Logger
import java.util.zip.GZIPInputStream

log = Logger.getLogger("")

def getEntry(line) {
    def pair = line.split('=')
    if(pair.size() != 2) {
        log.warn "found $line but it doesn't have a key and value"
        return
    }
    [(pair[0]) : pair[1]]
}

def getConfig(reader) {
    def lines = reader.readLines()
    def config = lines.findAll { line ->
        line.length() > 0 && !line.startsWith('#')
    }.inject([:]) { map, line ->
        map + getEntry(line)
    }
    
    config += lines.findAll { line ->
        line =~ /.+ is not set/
    }*.replace('# ', '')*.replace(' is not set', '')
    .inject([:]) { map, key ->
        map."$key" = 'is not set'
        map
    }
    
    config
}

def compare(target, source) {
    if(!source) {
        throw new NullPointerException('source is null')
    }
    compare = [:] + source

    target.each { k, v ->
        if(compare."$k" == v) {
            log.finest "removing $k"
            compare.remove(k)
        }
    }
    compare
}

def compareEquals(target, source) {
    compare = compare(target, source)
    
    target.each { k, v ->
        def sourceOn = compare."$k" == 'y' || compare."$k" == 'm'
        def targetOn = v == 'y' || v == 'm'
        
        if(sourceOn && targetOn) {
            compare.remove(k)
        }
    }
    compare
}

def removeNo(config) {
    config.findAll {k, v ->
        !(v == 'n' || v == 'is not set')
    }
}

def findStrings(config) {
    config.findAll { k, v ->
        !(v =='y' || v == 'm' || v == 'n')
    }
}

def findBoolean(config) {
    config.findAll { k, v ->
        (v =='y' || v == 'm' || v == 'n')
    }
}

def report(config, old) {
    config.collect { k, v ->
        def setTo
        if(v == 'is not set') {
            setTo = 'n'
        } else {
            setTo = v
        }
    "$k=$setTo #was ${old."$k"}"
    }.sort()
    .each {
        println it
    }

    println "${config.size()} config changes"
}

cli = new CliBuilder(usage:'compareConfig [options] source')
cli.t(longOpt:'target', args:1, argName:'target', 'change target from /proc/config.gz')
cli.c(longOpt:'compare', 'compares the source to the target printing what changes to apply to the target')
cli.e(longOpt:'equal', 'same as compare but m and y are equal')
cli.k(longOpt:'keep', 'report only configurations that are turned on in target')
cli.s(longOpt:'strings', 'report only strings (not set to y, m, n')
cli.b(longOpt:'boolean', 'report only y, m, n (not strings)')
options = cli.parse(args)

if(!options?.arguments()) {
    cli.usage()
    return
}

def targetReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream('/proc/config.gz'))))
def sourceReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File((options.arguments()[0])))))

if(options.target) {
    targetReader = new File(options.target)
}

def target = getConfig(targetReader)
def source = getConfig(sourceReader)

def result

if(options.c) {
    result = compare(target, source)
}

if(options.e) {
    result = compareEquals(target, source)
}

if(options.k) {
    result = removeNo(result)
}

if(options.s) {
    result = findStrings(result)
}

if(options.b) {
    result = findBoolean(result)
}

report(result, target)