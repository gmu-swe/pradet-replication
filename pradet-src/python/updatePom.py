#!/usr/local/bin/python
import sys, getopt
from cut.cut import CUT 

def main(argv):
    inputfile = None
    profilefile = None
    outputfile = None
    verbose = False
    forceJunitVersion = False
    
    try:
        opts, args = getopt.getopt(argv, "hvdi:p:o:", ["ifile=", "pfile=", "ofile="])
    except getopt.GetoptError:
        sys.exit(2)
    
    for opt, arg in opts:
        if opt == '-h':
            sys.exit()  # Help
        if opt == '-d':
            verbose = True
            sys.exit()
        elif opt in ("-i", "--ifile"):
            inputfile = arg
        elif opt in ("-o", "--ofile"):
            outputfile = arg
        elif opt in ("-p", "--pfile"):
            profilefile = arg
        if opt == '-v':  # Force JUnit Version
            forceJunitVersion = True
            
    print 'TEST'        
    if inputfile is None:
        sys.exit(1)
            
    # print 'Input file is "', inputfile
    if profilefile is None:
        cut = CUT(profilefile)
    else:
        cut = CUT(profilefile)
    
    if forceJunitVersion:
        updatePom = cut.updateJUnitVersion(inputfile)
    else:
        updatePom = cut.injectCutProfiles(inputfile)
    # TODO Print to file if necessary
    
    if outputfile is None:
        print(cut.prettify(updatePom))
    else:
        with open(outputfile, "w") as f:
            f.write(cut.prettify(updatePom))
        
    
if __name__ == "__main__":
    main(sys.argv[1:])
